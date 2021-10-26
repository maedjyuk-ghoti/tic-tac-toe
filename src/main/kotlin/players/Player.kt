package players

import Action
import GameError
import GameState
import IO
import InputError
import PlayerInfo
import com.github.michaelbull.result.*
import getLine
import put

sealed interface Player {
    fun getAction(gameState: GameState): IO<Result<Action, GameError>>

    class Human(
        private val playerInfo: PlayerInfo,
    ): Player {

        override fun getAction(gameState: GameState): IO<Result<Action, GameError>> {
            return requestInput(playerInfo.name)
                .map { input -> input.andThen(Action.Companion::parse) }
        }

        /**
         * Print a string and retrieve input from the command line.
         *
         * @param name A name by which to address the player
         * @return A [Result] containing the [String] entered by the [PlayerInfo] or a [Throwable]
         */
        private fun requestInput(name: String): IO<Result<String, InputError>> {
            return put("Player %s's turn: ".format(name))
                .flatMap { getLine() }
        }
    }

    class Bot(
        private val playerInfo: PlayerInfo,
        private val botStrategy: BotStrategy
    ) : Player {
        override fun getAction(gameState: GameState): IO<Result<Action, GameError>> {
            return botStrategy.getCoordinates(gameState, playerInfo)
                .fold({ coordinates ->
                    put("Player %s's turn (Bot): %s".format(playerInfo.name, "m ${coordinates.x} ${coordinates.y}"))
                        .map { Ok(Action.Move(coordinates)) }
                }, { error ->
                    IO { Err(error) }
                })
        }
    }
}