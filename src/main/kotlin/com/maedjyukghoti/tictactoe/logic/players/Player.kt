package com.maedjyukghoti.tictactoe.logic.players

import com.github.michaelbull.result.fold
import com.maedjyukghoti.tictactoe.AppState.Game
import com.maedjyukghoti.tictactoe.UserIntent
import com.maedjyukghoti.tictactoe.logic.PlayerInfo

sealed interface Player {
    data object Human : Player

    data class Bot(
        private val playerInfo: PlayerInfo,
        private val botStrategy: BotStrategy,
    ) : Player {
        fun getAction(gameState: Game): UserIntent {
            return botStrategy.getCoordinates(gameState, playerInfo)
                .fold(
                    success = { coordinates -> UserIntent.Move(coordinates) },
                    failure = { moveError -> UserIntent.Error(moveError) },
                )
        }
    }
}
