package com.maedjyukghoti.tictactoe.display

import com.github.michaelbull.result.*
import com.maedjyukghoti.tictactoe.*
import com.maedjyukghoti.tictactoe.logic.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.log10

sealed interface UserInterface {
    /**
     * Observable to monitor user interactions on the screen
     */
    val userIntent: Flow<UserIntent>

    /**
     * Update the display to show the current app state
     */
    fun render(state: AppState)

    /**
     * A programmatic way of interacting with this UI
     */
    fun interact(intent: UserIntent)

    /**
     * Cleans up any background work or the UI
     */
    suspend fun destroy()

    /**
     * Represents a command line interface
     */
    class CLI(scope: CoroutineScope) : UserInterface {
        private val _userIntent: MutableSharedFlow<UserIntent> = MutableSharedFlow()
        override val userIntent: Flow<UserIntent> = _userIntent.asSharedFlow()

        /**
         * Start a job to listen to the CLI for user input
         */
        private val job = scope.launch {
            while (isActive) {
                val input = readlnOrNull()
                if (input != null) {
                    _userIntent.emit(parseInput(input))
                } else {
                    // handle EOF
                }
            }
        }

        override fun render(state: AppState) {
            when (state) {
                AppState.Exit -> TODO()
                AppState.FatalError -> TODO()
                is AppState.Game -> handleGame(state)
            }
        }

        private fun handleGame(state: AppState.Game) {
            println(drawBoard(state.board))
            println(actionsText)

            if (state.winner != PlayerInfo.None) {
                println("Player ${state.winner} wins!")
            }
            if (state.isTied) {
                println("No more moves available. Game is a tie")
            }
            if (state.error != null) {
                println(getErrorString(state.error))
            }
        }

        override fun interact(intent: UserIntent) {
            TODO("Not yet implemented")
        }

        override suspend fun destroy() {
            println("") // emit a character to trigger job to read line and recheck isActive
            job.cancel()
        }

        /**
         * Parse user input into Intent
         */
        private fun parseInput(input: String): UserIntent =
            when (input[0]) {
                'm' -> parseCoordinates(input.substring(1).trim())
                    .fold(
                        success = { UserIntent.Move(it) },
                        failure = { UserIntent.Error(it) }
                    )

                'u' -> runCatching { input.substring(1).trim().toInt() }
                    .fold(
                        success = { UserIntent.Undo(it) },
                        failure = { UserIntent.Undo(1) }
                    )

                'q' -> UserIntent.Quit

                else -> UserIntent.Error(InputError.InvalidAction(input))
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
                |   Move: m <x y>
                |   Undo: u [#]
                |   Quit: q
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

            private fun buildGameOptionsErrorMessage(gameOptionsError: GameOptionsError): String =
                listOfNotNull(
                    if (gameOptionsError.boardSize.second) "board size (${gameOptionsError.boardSize.first})" else null,
                    if (gameOptionsError.numberOfHumans.second) "number of humans (${gameOptionsError.numberOfHumans.first})" else null,
                    if (gameOptionsError.humanPosition.second) "human position (${gameOptionsError.humanPosition.first})" else null,
                    if (gameOptionsError.botLevel.second) "bot level (${gameOptionsError.botLevel.first})" else null
                ).joinToString(separator = ", ", prefix = "Invalid options: ")

            /**
             * Parse a string for coordinates
             *
             * @param input A string that may contain usable info for com.maedjyukghoti.tictactoe.tictactoe
             * @return A [Result] containing the [Coordinates] entered by the [PlayerInfo] or a [Throwable]
             */
            fun parseCoordinates(input: String): Result<Coordinates, InputError> {
                val split = input.split(" ")
                if (split.size != 2) return Err(InputError.InvalidCoordinates(input))

                val x = split[0].toIntOrNull() ?: return Err(InputError.InvalidCoordinates(input))
                val y = split[1].toIntOrNull() ?: return Err(InputError.InvalidCoordinates(input))

                return Ok(Coordinates(x, y))
            }
        }
    }
}