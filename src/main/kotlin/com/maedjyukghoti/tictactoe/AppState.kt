package com.maedjyukghoti.tictactoe

import com.github.michaelbull.result.fold
import com.github.michaelbull.result.map
import com.maedjyukghoti.tictactoe.logic.Board
import com.maedjyukghoti.tictactoe.logic.GameOptions
import com.maedjyukghoti.tictactoe.logic.MoveRequest
import com.maedjyukghoti.tictactoe.logic.PlayerInfo
import com.maedjyukghoti.tictactoe.logic.checkForWinner
import com.maedjyukghoti.tictactoe.logic.players.BotStrategy
import com.maedjyukghoti.tictactoe.logic.players.Player

sealed interface AppState {
    fun handleAction(intent: UserIntent): AppState

    data class Game(
        val board: Board,
        val players: Map<PlayerInfo, Player>,
        val currentPlayerInfo: PlayerInfo,
        val winner: PlayerInfo,
        val isTied: Boolean,
        val error: GameError?,
    ) : AppState {
        override fun handleAction(intent: UserIntent): AppState =
            when (intent) {
                is UserIntent.Move -> move(intent)
                is UserIntent.Undo -> undo(intent)
                is UserIntent.Error -> copy(error = intent.error)
                UserIntent.Quit -> Exit
            }

        private fun move(intent: UserIntent.Move): AppState =
            board.validate(MoveRequest(intent.coordinates, currentPlayerInfo))
                .map { request -> board.makeMove(request) }
                .fold(
                    success = { updatedBoard -> update(updatedBoard, PlayerInfo.nextPlayer(currentPlayerInfo)) },
                    failure = { moveError -> copy(error = moveError) },
                )

        private fun undo(intent: UserIntent.Undo): AppState =
            board.undoMove(intent.count)
                .fold(
                    success = { updatedBoard -> update(updatedBoard, PlayerInfo.backUp(currentPlayerInfo, intent.count)) },
                    failure = { gameError -> copy(error = gameError) },
                )

        private fun update(
            newBoard: Board,
            newPlayer: PlayerInfo,
        ): Game =
            Game(
                board = newBoard,
                players = players,
                currentPlayerInfo = newPlayer,
                winner = checkForWinner(newBoard),
                isTied = newBoard.moves.count() == newBoard.totalMovesAllowed(),
                error = null,
            )

        fun getCurrentPlayer(): Player = players.getValue(currentPlayerInfo)

        companion object {
            fun createNewGame(gameOptions: GameOptions): Game =
                Game(
                    board = Board(emptyList(), gameOptions.boardSize),
                    players = getPlayers(gameOptions.numberOfHumans, gameOptions.humanPosition, gameOptions.botLevel),
                    currentPlayerInfo = PlayerInfo.One,
                    winner = PlayerInfo.None,
                    isTied = false,
                    error = null,
                )

            private fun getPlayers(
                numberOfHumans: Int,
                humanPosition: Int,
                botLevel: Int,
            ): Map<PlayerInfo, Player> {
                return if (numberOfHumans == 2) {
                    return mapOf(
                        PlayerInfo.One to Player.Human,
                        PlayerInfo.Two to Player.Human,
                    )
                } else {
                    val humanInfo = if (humanPosition == 1) PlayerInfo.One else PlayerInfo.Two
                    val botInfo = PlayerInfo.nextPlayer(humanInfo)
                    mapOf(
                        humanInfo to Player.Human,
                        botInfo to Player.Bot(botInfo, BotStrategy.getBotAtLevel(botLevel)),
                    )
                }
            }
        }
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
