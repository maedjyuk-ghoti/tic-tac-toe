package com.maedjyukghoti.tictactoe.display

import com.github.michaelbull.result.*
import com.maedjyukghoti.tictactoe.logic.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.log10

sealed interface Screen {
    data object Welcome : Screen

    data class Options(
        val gameOptions: GameOptions?,
        val invalidGameOptions: InvalidGameOptions?
    ) : Screen

    data class Game(
        val board: Board,
        val winner: PlayerInfo,
        val isTied: Boolean,
        val error: GameError?
    ) : Screen
}

sealed interface Display {
    val actions: Flow<Result<Action, InputError>>
    fun display(screen: Screen)

    /** A way for bots to play the game */
    fun interact(action: Action)

    data class CLI(
        private val scope: CoroutineScope,
        private val onAction: (String?) -> Result<Action, InputError>,
    ) : Display {
        private val _actions: MutableSharedFlow<Result<Action, InputError>> = MutableSharedFlow()
        override val actions: Flow<Result<Action, InputError>> = _actions.asSharedFlow()

        override fun display(screen: Screen) {
            when (screen) {
                is Screen.Welcome -> handleWelcome(screen)
                is Screen.Options -> handleOptions(screen)
                is Screen.Game -> handleGame(screen)
            }
        }

        fun handleWelcome(welcome: Screen.Welcome) {
            println("Welcome to Tic-Tac-Toe!")
        }

        fun handleOptions(options: Screen.Options) {
            println("You are now in options: $options")
        }

        fun handleGame(game: Screen.Game) {
            println(drawBoard(game.board))
            println(actionsText)

            if (game.winner != PlayerInfo.None) { println("Player ${game.winner} wins!") }
            if (game.isTied) { println("No more moves available. Game is a tie") }
            if (game.error != null) { println(getErrorString(game.error)) }

            scope.launch {
                _actions.emit(onAction(readlnOrNull()))
            }
        }

        override fun interact(action: Action) {
            val actionString = when (action) {
                is Action.Move -> "m ${action.coordinates.x} ${action.coordinates.y}"
                is Action.Undo -> "u ${action.times}"
            }
            println(actionString)

            scope.launch {
                _actions.emit(Ok(action))
            }
        }

        companion object {
            val optionsText = """
                | Options:
                |   --board-size 3
                |       Board size for NxN board.
                |   --number-of-humans 2
                |       Number of human players this game (limit of 2).
                |   --human-position 1
                |       If only 1 human player, do you want to go first or second?
                |   --bot-level 3
                |       Difficulty of the AI. Available levels 0-3
            """.trimMargin()

            val actionsText = """
                | Actions:
                |   Move: m [x y]
                |   Undo: u
            """.trimMargin()

            /**
             * Transform the board into a string for display
             *  0,0 is the bottom left corner
             *  n,n is the top right corner
             */
            fun drawBoard(board: Board): String {
                val lastIndex = board.bounds - 1
                val buffer = StringBuilder("\n")
                val grid = board.moves.associate { request -> request.coordinates to request.playerInfo }

                for (i in lastIndex downTo 0) {
                    // draw row number
                    buffer.append(appendWithSpaceAfter(2, i))

                    for (j in 0..lastIndex) {
                        val square = grid[Coordinates(j, i)] ?: PlayerInfo.None
                        // draw square and column separator
                        when (j) {
                            lastIndex -> buffer.append(" ${square.symbol()}")
                            else -> buffer.append(" ${square.symbol()} |")
                        }
                    }

                    // draw row separator
                    when (i) {
                        0 -> buffer.append("\n")
                        else -> {
                            buffer.append("\n  ")
                            for (k in 0 until ((board.bounds * 4) - 1)) buffer.append('-')
                            buffer.append("\n")
                        }
                    }
                }

                // draw column number
                for (h in 0..lastIndex) {
                    buffer.append(appendWithSpaceBefore(4, h))
                }
                buffer.append("\n")

                return buffer.toString()
            }

            private fun PlayerInfo.symbol(): Char =
                when (this) {
                    PlayerInfo.None -> ' '
                    PlayerInfo.One -> 'X'
                    PlayerInfo.Two -> 'O'
                }

            fun appendWithSpaceBefore(spaceCount: Int, number: Int): String {
                val numberOfSpaces = spaceCount - getMagnitude(number)

                return if (numberOfSpaces < 0) ""
                else StringBuilder()
                    .appendSpaces(numberOfSpaces) // spaces before number
                    .append(number)
                    .toString()
            }

            fun appendWithSpaceAfter(spaceCount: Int, number: Int): String {
                val numberOfSpaces = spaceCount - getMagnitude(number)

                return if (numberOfSpaces < 0) ""
                else StringBuilder()
                    .append(number)
                    .appendSpaces(numberOfSpaces) // spaces after number
                    .toString()
            }

            /**
             * log10(0) is undefined, make a special case for it
             */
            private fun getMagnitude(number: Int): Int =
                if (number == 0) 1
                else log10(number.toDouble()).toInt() + 1

            private fun StringBuilder.appendSpaces(numberOfSpaces: Int): StringBuilder {
                (0 until numberOfSpaces).forEach { _ -> append(' ') }
                return this
            }

            fun getErrorString(gameError: GameError): String =
                when (gameError) {
                    is GameOptionsError -> buildGameOptionsErrorMessage(gameError)
                    is InputError.InvalidAction -> "Invalid input: ${gameError.input}"
                    is InputError.InvalidCoordinates -> "Invalid Coordinates: ${gameError.input}"
                    InputError.MissingInput -> "No input received"
                    MoveError.CoordinateTaken -> "That square has already been played"
                    is MoveError.InvalidCoordinates -> "Invalid coordinates: ${gameError.coordinates}"
                    MoveError.NoAvailableMoves -> "No available moves"
                    MoveError.NoWinningMove -> "No winning move"
                    UndoError.NoMovesToUndo -> "No moves to undo"
                    UndoError.RequestTooLarge -> "Requested more moves than are present"
                }

            fun buildGameOptionsErrorMessage(gameOptionsError: GameOptionsError): String =
                listOfNotNull(
                    if (gameOptionsError.boardSize.second) "board size (${gameOptionsError.boardSize.first})" else null,
                    if (gameOptionsError.numberOfHumans.second) "number of humans (${gameOptionsError.numberOfHumans.first})" else null,
                    if (gameOptionsError.humanPosition.second) "human position (${gameOptionsError.humanPosition.first})" else null,
                    if (gameOptionsError.botLevel.second) "bot level (${gameOptionsError.botLevel.first})" else null
                ).joinToString(separator = ", ", prefix = "Invalid options: ")
        }
    }
}

fun main() = runBlocking {
    val ioScope = CoroutineScope(Dispatchers.IO)
    val display = Display.CLI(ioScope) { _ -> Ok(Action.Move(Coordinates(0, 0))) }

    display.display(Screen.Welcome)

    display.actions.onEach { _ ->
        display.display(Screen.Options(null, null))
    }.launchIn(ioScope)

    println("Waiting for input")
    delay(5000)
    println("End")
}