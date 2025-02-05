package com.maedjyukghoti.tictactoe.logic.players

import com.github.michaelbull.result.*
import com.maedjyukghoti.tictactoe.UserIntent
import com.maedjyukghoti.tictactoe.logic.*

sealed interface Player {
    data object Human: Player

    class Bot(
        private val playerInfo: PlayerInfo,
        private val botStrategy: BotStrategy
    ) : Player {
        fun getAction(gameState: GameState): UserIntent {
            return botStrategy.getCoordinates(gameState, playerInfo)
                .fold(
                    success = { coordinates -> UserIntent.Move(coordinates) },
                    failure = { moveError -> UserIntent.Error(moveError) }
                )
        }
    }
}