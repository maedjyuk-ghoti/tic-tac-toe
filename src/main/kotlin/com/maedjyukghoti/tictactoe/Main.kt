package com.maedjyukghoti.tictactoe

import com.github.michaelbull.result.*
import com.maedjyukghoti.tictactoe.display.Display
import com.maedjyukghoti.tictactoe.display.Screen
import com.maedjyukghoti.tictactoe.logic.*
import com.maedjyukghoti.tictactoe.logic.players.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun getopt(args: Array<String>): Map<String, List<String>> {
    return args.fold(mutableListOf()) { acc: MutableList<MutableList<String>>, s: String ->
        acc.apply {
            if (s.startsWith('-')) add(mutableListOf(s))
            else last().add(s)
        }
    }.associate { it[0] to it.drop(1) }
}

fun main(args: Array<String>) = runBlocking {
    val opts = getopt(args)
    val scope = CoroutineScope(Dispatchers.IO)
    val display = Display.CLI(scope) { input ->
        if (!input.isNullOrEmpty()) Action.parse(input)
        else Err(InputError.MissingInput)
    }

    GameOptions.parse(opts)
        .fold(
            success = { gameOptions ->
                tictactoe(gameOptions, display, scope)
            },
            failure = { invalidGameOptions ->
                display.display(Screen.Options(null, invalidGameOptions))
            }
        )

    while (scope.isActive) {
        delay(1000)
    }

    println("main is ending")
}

/** Start a game of tic-tac-toe **/
fun tictactoe(gameOptions: GameOptions, display:Display, scope: CoroutineScope) {
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
}

fun getPlayers(numberOfHumans: Int, humanPosition: Int, botLevel: Int): Map<PlayerInfo, Player> {
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