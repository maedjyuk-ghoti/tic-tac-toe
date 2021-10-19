package players

import Action
import Coordinates
import GameState
import PlayerInfo
import com.github.michaelbull.result.*

sealed class Player {
    abstract fun getAction(gameState: GameState): Result<Action, Throwable>

    class Human(
        private val playerInfo: PlayerInfo,
        private val readIn: () -> String?,
        private val printOut: (String) -> Unit
    ): Player() {

        override fun getAction(gameState: GameState): Result<Action, Throwable> {
            return requestInput(playerInfo.name, printOut, readIn)
                .andThen { input -> parseAction(input) }
        }

        /**
         * Print a string and retrieve input from the command line.
         *
         * @param name A name by which to address the player
         * @param printOut A method to print a string out
         * @param readIn A method that reads a string in
         * @return A [Result] containing the [String] entered by the [PlayerInfo] or a [Throwable]
         */
        private fun requestInput(name: String, printOut: (String) -> Unit, readIn: () -> String?): Result<String, Throwable> {
            printOut(name)
            val input = readIn() ?: return Err(Throwable("No input received"))
            return Ok(input)
        }

        private fun parseAction(input: String): Result<Action, Throwable> {
            return when (input[0]) {
                'm' -> Coordinates.parse(input.substring(1).trim()).map { Action.Move(it) }
                'u' -> Ok(Action.Undo)
                else -> Err(Throwable("Invalid input"))
            }
        }
    }

    class Bot(
        private val playerInfo: PlayerInfo,
        private val printOut: (String, String) -> Unit,
        private val botStrategy: BotStrategy
    ) : Player() {
        override fun getAction(gameState: GameState): Result<Action, Throwable> {
            return botStrategy.getCoordinates(gameState)
                .onSuccess { printOut(playerInfo.name, "m ${it.x} ${it.y}") }
                .map { coordinates -> Action.Move(coordinates) }
        }
    }
}