package com.maedjyukghoti.tictactoe

import com.github.michaelbull.result.*
import com.maedjyukghoti.tictactoe.display.*
import com.maedjyukghoti.tictactoe.logic.*
import com.maedjyukghoti.tictactoe.logic.players.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds

fun getopt(args: Array<String>): Map<String, List<String>> {
    return args.fold(mutableListOf()) { acc: MutableList<MutableList<String>>, s: String ->
        acc.apply {
            if (s.startsWith('-')) add(mutableListOf(s))
            else last().add(s)
        }
    }.associate { it[0] to it.drop(1) }
}

sealed interface AppState {
    fun handleAction(intent: UserIntent): AppState

    data class Game(
        val board: Board,
        val players: Map<PlayerInfo, Player>,
        val currentPlayerInfo: PlayerInfo,
        val winner: PlayerInfo,
        val isTied: Boolean,
        val error: GameError?
    ) : AppState {
        private fun update(newBoard: Board, newPlayer: PlayerInfo): Game =
            Game(
                board = newBoard,
                players = players,
                currentPlayerInfo = newPlayer,
                winner = checkForWinner(newBoard),
                isTied = newBoard.moves.count() == newBoard.totalMovesAllowed(),
                error = null
            )

        override fun handleAction(intent: UserIntent): AppState =
            when (intent) {
                is UserIntent.Move -> move(intent)
                is UserIntent.Undo -> undo(intent)
                is UserIntent.Error -> copy(error = intent.error)
                UserIntent.Quit -> Exit
            }

        private fun move(intent: UserIntent.Move): AppState =
            validate(MoveRequest(intent.coordinates, currentPlayerInfo), board)
                .map { request -> makeMove(request, board) }
                .fold(
                    success = { updatedBoard -> update(updatedBoard, PlayerInfo.nextPlayer(currentPlayerInfo)) },
                    failure = { moveError -> copy(error = moveError) }
                )

        private fun undo(intent: UserIntent.Undo): AppState =
            board.undoMove(intent.count)
                .fold(
                    success = { updatedBoard -> update(updatedBoard, PlayerInfo.backUp(currentPlayerInfo, intent.count)) },
                    failure = { gameError -> copy(error = gameError) }
                )
    }

    data object FatalError : AppState {
        // No intent can recover at this time
        override fun handleAction(intent: UserIntent): AppState = this
    }

    data object Exit : AppState {
        // No intent can recover at this time
        override fun handleAction(intent: UserIntent): AppState = this
    }
}

sealed interface UserIntent {
    data class Move(val coordinates: Coordinates): UserIntent
    data class Undo(val count: Int) : UserIntent
    data class Error(val error: GameError) : UserIntent
    data object Quit : UserIntent
}

fun reducer(state: AppState, intent: UserIntent): AppState =
    state.handleAction(intent)

private fun getPlayers(numberOfHumans: Int, humanPosition: Int, botLevel: Int): Map<PlayerInfo, Player> {
    return if (numberOfHumans == 2) {
        return mapOf(
            PlayerInfo.One to Player.Human,
            PlayerInfo.Two to Player.Human
        )
    } else {
        val humanInfo = if (humanPosition == 1) PlayerInfo.One else PlayerInfo.Two
        val botInfo = PlayerInfo.nextPlayer(humanInfo)
        mapOf(
            humanInfo to Player.Human,
            botInfo to Player.Bot(botInfo, BotStrategy.getBotAtLevel(botLevel))
        )
    }
}

private fun createNewGame(gameOptions: GameOptions): AppState.Game =
    AppState.Game(
        board = Board(emptyList(), gameOptions.boardSize),
        players = getPlayers(gameOptions.numberOfHumans, gameOptions.humanPosition, gameOptions.botLevel),
        currentPlayerInfo = PlayerInfo.One,
        winner = PlayerInfo.None,
        isTied = false,
        error = null
    )

fun main(args: Array<String>) = runBlocking {
    val gameOptions: Result<GameOptions, InvalidGameOptions> = GameOptions.parse(getopt(args))
    val initialState: AppState = gameOptions.fold(
        success = { validOptions -> createNewGame(validOptions) },
        failure = { _ -> AppState.FatalError }
    )
    val state: MutableStateFlow<AppState> = MutableStateFlow(initialState)

    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val ui: UserInterface = UserInterface.CLI(coroutineScope)

    launch {
        state.zip(ui.userIntent, ::Pair)
            .takeWhile { (appState, _) -> appState != AppState.Exit }
            .map { (_, userIntent) -> userIntent }
            .collect { userIntent -> state.value = reducer(state.value, userIntent) }
    }

    launch {
        state.takeWhile { appState -> appState != AppState.Exit }
            .filter { appState -> appState == AppState.FatalError }
            .collect { _ ->
                delay(5.seconds)
                state.value = AppState.Exit
            }
    }

    launch {
        state.takeWhile { appState -> appState != AppState.Exit }
            .collect { appState -> ui.render(appState) }
    }

    launch {
        state.filter { appState -> appState == AppState.Exit }
            .take(1)
            .collect {
                coroutineScope.cancel()
                ui.destroy()
            }
    }

    Unit // main must return Unit to compile correctly as a main function
}

/** Start a game of tic-tac-toe **/
/*fun tictactoe(gameOptions: GameOptions, display:Display, scope: CoroutineScope) {
    // start game flow with initial game state
    val gameStateFlow: MutableStateFlow<GameState> =
        MutableStateFlow(
            GameState(
                board = Board(emptyList(), gameOptions.boardSize),
                players = getPlayers(gameOptions.numberOfHumans, gameOptions.humanPosition, gameOptions.botLevel),
                currentPlayerInfo = PlayerInfo.One,
                winner = PlayerInfo.None,
                error = null
            )
        )


    // start updating display based on game state
    gameStateFlow.onEach { gameState ->
        val gameScreen: Screen.Game = Screen.Game(
            board = gameState.board,
            winner = gameState.winner,
            isTied = gameState.board.moves.count() == gameState.board.totalMovesAllowed(),
            error = gameState.error
        )

        display.display(gameScreen)
    }.takeWhile { gameState -> gameState.winner == PlayerInfo.None }
        .launchIn(scope)

    // let the bot take a turn
    gameStateFlow
        .takeWhile { _ -> gameStateFlow.value.winner == PlayerInfo.None }
        .onEach { gameState ->
        delay(1000)
        (gameState.getCurrentPlayer().takeIf { it is Player.Bot } as Player.Bot?)
            ?.getAction(gameState)
            ?.map(display::interact)
    }.launchIn(scope)

    // play game on loop
    display.actions
        .onEach { actionResult ->
            actionResult.flatMap { action -> action.act(gameStateFlow.value) }
                .mapBoth(
                    success = { newGameState -> gameStateFlow.value = newGameState.copy(error = null) },
                    failure = { gameError -> gameStateFlow.value = gameStateFlow.value.copy(error = gameError) }
                )
        }.takeWhile { _ -> gameStateFlow.value.winner == PlayerInfo.None }
        .launchIn(scope)
}*/