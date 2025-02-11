package com.maedjyukghoti.tictactoe.logic.players

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.orElse
import com.github.michaelbull.result.runCatching
import com.maedjyukghoti.tictactoe.AppState.Game
import com.maedjyukghoti.tictactoe.MoveError
import com.maedjyukghoti.tictactoe.logic.Board
import com.maedjyukghoti.tictactoe.logic.Coordinates
import com.maedjyukghoti.tictactoe.logic.MoveRequest
import com.maedjyukghoti.tictactoe.logic.PlayerInfo
import com.maedjyukghoti.tictactoe.logic.checkForWinner

sealed interface BotStrategy {
    fun getCoordinates(
        gameState: Game,
        botPlayer: PlayerInfo,
    ): Result<Coordinates, MoveError>

    companion object {
        fun getBotAtLevel(botLevel: Int): BotStrategy =
            when (botLevel) {
                1 -> OneLayer
                2 -> TwoLayer
                3 -> MiniMax
                else -> Random
            }
    }

    data object Random : BotStrategy {
        override fun getCoordinates(
            gameState: Game,
            botPlayer: PlayerInfo,
        ): Result<Coordinates, MoveError> = getRandomCoordinates(gameState.board)
    }

    data object OneLayer : BotStrategy {
        override fun getCoordinates(
            gameState: Game,
            botPlayer: PlayerInfo,
        ): Result<Coordinates, MoveError> =
            getWinningCoordinates(gameState.board, botPlayer)
                .orElse { getRandomCoordinates(gameState.board) }
    }

    /**
     * Because of the blocking move, only works for 2 players.
     * What if the blocking move was evaluated for all players?
     *  1) Make list of all players starting from next player excluding current player
     *  2) first to have a blocking move returns
     *  3) else no blocking move
     */
    data object TwoLayer : BotStrategy {
        override fun getCoordinates(
            gameState: Game,
            botPlayer: PlayerInfo,
        ): Result<Coordinates, MoveError> =
            getWinningCoordinates(gameState.board, botPlayer)
                .orElse { getBlockingCoordinates(gameState.board, PlayerInfo.nextPlayer(botPlayer)) }
                .orElse { getRandomCoordinates(gameState.board) }
    }

    data object MiniMax : BotStrategy {
        private val memo: MutableMap<Set<MoveRequest>, Choice> = mutableMapOf()

        override fun getCoordinates(
            gameState: Game,
            botPlayer: PlayerInfo,
        ): Result<Coordinates, MoveError> =
            Ok(
                minimax(
                    board = gameState.board,
                    myPlayer = botPlayer,
                    currentPlayer = gameState.currentPlayerInfo,
                    depth = 0,
                    memo = memo,
                ).option,
            )
    }
}

fun getRandomCoordinates(board: Board): Result<Coordinates, MoveError> =
    runCatching { board.getRemainingCoordinates().random() }
        .mapError { MoveError.NoAvailableMoves }

/** Return the [Coordinates] to block [PlayerInfo] if they exist **/
fun getBlockingCoordinates(
    board: Board,
    playerInfo: PlayerInfo,
): Result<Coordinates, MoveError> = getWinningCoordinates(board, playerInfo)

/** Return the winning [Coordinates] for [PlayerInfo] on the [Board] if they exist. **/
fun getWinningCoordinates(
    board: Board,
    playerInfo: PlayerInfo,
): Result<Coordinates, MoveError> =
    runCatching {
        board.getRemainingCoordinates()
            .first { coordinates ->
                val moveRequest = MoveRequest(coordinates, playerInfo)
                val newBoard = board.makeMove(moveRequest)
                val winner = checkForWinner(newBoard)
                winner == playerInfo
            }
    }.mapError { MoveError.NoWinningMove }

data class Choice(val option: Coordinates, val value: Int, val depth: Int) : Comparable<Choice> {
    override fun compareTo(other: Choice): Int =
        if (value > other.value) {
            1
        } else if (value < other.value) {
            -1
        } else {
            0
        }
}

fun minimax(
    board: Board,
    myPlayer: PlayerInfo,
    currentPlayer: PlayerInfo,
    depth: Int,
    memo: MutableMap<Set<MoveRequest>, Choice>,
): Choice {
    val winner = checkForWinner(board)
    if (winner == myPlayer) {
        return Choice(board.moves.last().coordinates, (board.totalMovesAllowed() + 1) - depth, depth)
    } else if (winner != PlayerInfo.None) {
        return Choice(board.moves.last().coordinates, ((board.totalMovesAllowed() + 1) * -1) + depth, depth)
    } else if (board.moves.count() == board.totalMovesAllowed()) {
        return Choice(board.moves.last().coordinates, 0, depth)
    }

    val choice =
        if (memo.contains(board.moves.toSet())) {
            memo.getValue(board.moves.toSet())
        } else {
            val compare: (Choice, Choice) -> Choice = if (myPlayer == currentPlayer) ::maxOf else ::minOf
            board.getRemainingCoordinates()
                .map { coordinates ->
                    val moveRequest = MoveRequest(coordinates, currentPlayer)
                    val updateBoard = board.makeMove(moveRequest)
                    val nextPlayer = PlayerInfo.nextPlayer(currentPlayer)
                    val result = minimax(updateBoard, myPlayer, nextPlayer, depth + 1, memo)
                    result.copy(option = updateBoard.moves.last().coordinates)
                }.reduce(compare)
        }

    memo[board.moves.toSet()] = choice
    return choice
}
