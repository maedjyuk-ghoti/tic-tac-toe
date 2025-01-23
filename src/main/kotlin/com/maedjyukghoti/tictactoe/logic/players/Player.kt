package com.maedjyukghoti.tictactoe.logic.players

import com.github.michaelbull.result.*
import com.maedjyukghoti.tictactoe.logic.*

sealed interface Player {
    data object Human: Player

    class Bot(
        private val playerInfo: PlayerInfo,
        private val botStrategy: BotStrategy
    ) : Player {
        fun getAction(gameState: GameState): Result<Action, GameError> {
            return botStrategy.getCoordinates(gameState, playerInfo)
                .map { coordinates -> Action.Move(coordinates) }
        }
    }
}