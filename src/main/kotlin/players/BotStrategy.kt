package players

import Board
import Coordinates
import GameState
import MoveRequest
import PlayerInfo
import com.github.michaelbull.result.*
import com.sun.source.tree.BinaryTree
import makeMove
import java.util.*
import kotlin.collections.fold

sealed interface BotStrategy {
    fun getCoordinates(gameState: GameState): Result<Coordinates, Throwable>

    companion object {
        fun getBotAtLevel(botLevel: Int): BotStrategy {
            return when (botLevel) {
                1 -> OneLayer
                2 -> TwoLayer
                else -> Random
            }
        }
    }

    object Random : BotStrategy {
        override fun getCoordinates(gameState: GameState): Result<Coordinates, Throwable> {
            return getRandomCoordinates(gameState.board)
        }
    }

    object OneLayer : BotStrategy {
        override fun getCoordinates(gameState: GameState): Result<Coordinates, Throwable> {
            return getWinningCoordinates(gameState.board, gameState.currentPlayerInfo)
                .orElse { getRandomCoordinates(gameState.board) }
        }
    }

    /**
     * Because of the blocking move, only works for 2 players.
     * What if the blocking move was evaluated for all players?
     *  1) Make list of all players starting from next player excluding current player
     *  2) first to have a blocking move returns
     *  3) else no blocking move
     */
    object TwoLayer : BotStrategy {
        override fun getCoordinates(gameState: GameState): Result<Coordinates, Throwable> {
            return getWinningCoordinates(gameState.board, gameState.currentPlayerInfo)
                .orElse { getBlockingCoordinates(gameState.board, PlayerInfo.nextPlayer(gameState.currentPlayerInfo)) }
                .orElse { getRandomCoordinates(gameState.board) }
        }
    }

    object Perfect : BotStrategy {
        override fun getCoordinates(gameState: GameState): Result<Coordinates, Throwable> {
            TODO("Not yet implemented")
        }
    }

    object MiniMax : BotStrategy {
        override fun getCoordinates(gameState: GameState): Result<Coordinates, Throwable> {
            TODO("Not yet implemented")
        }
    }
}

fun getRandomCoordinates(board: Board): Result<Coordinates, Throwable> {
    return runCatching { board.getRemainingCoordinates().random() }
        .mapError { Throwable("No available moves") }
}

/**
 * Returns the [Coordinates] to block [PlayerInfo] if they exist
 */
fun getBlockingCoordinates(board: Board, playerInfo: PlayerInfo): Result<Coordinates, Throwable> {
    return getWinningCoordinates(board, playerInfo)
}

/**
 * Return the winning [Coordinates] for [PlayerInfo] if they exist.
 */
fun getWinningCoordinates(board: Board, playerInfo: PlayerInfo): Result<Coordinates, Throwable> {
    return runCatching {
        board.getRemainingCoordinates()
            .first { coordinates ->
                val moveRequest = MoveRequest(coordinates, playerInfo, board.getNextMoveNumber())
                val newBoard = makeMove(moveRequest, board)
                val winner = newBoard.checkForWinner()
                winner == playerInfo
            }
    }.mapError { Throwable("No winning move") }
}

// TODO rewrite check for winner with this setup. Looks much easier to understand.
fun getWinningCoordinates2(board: Board, playerInfo: PlayerInfo): Result<Coordinates, Throwable> {
    val myMoves = board.moves.filter { request -> request.playerInfo == playerInfo }
    val moves = myMoves.associateBy { request -> request.coordinates }
    val magicNumber = List(board.bounds) { it + 1 }.fold(0) { acc, i -> acc + i }

    for (i in 0 until board.bounds) {
        val columnSquares = moves.filter { (coordinates, _) -> coordinates.x == i }
        if (columnSquares.count() == board.bounds - 1) {
            val partialMagicNumber = columnSquares.map { (coordinates, _) -> coordinates.y + 1 }.fold(0) { acc, n -> acc + n }
            val missingY = magicNumber - partialMagicNumber - 1
            val coordinates = Coordinates(i, missingY)
            if (!board.moves.map(MoveRequest::coordinates).contains(coordinates)) return Ok(coordinates)
        }

        val rowSquares = moves.filter { (coordinates, _) -> coordinates.y == i}
        if (rowSquares.count() == board.bounds - 1) {
            val partialMagicNumber = rowSquares.map { (coordinates, _) -> coordinates.x + 1 }.fold(0) { acc, n -> acc + n }
            val missingX = magicNumber - partialMagicNumber - 1
            val coordinates = Coordinates(missingX, i)
            if (!board.moves.map(MoveRequest::coordinates).contains(coordinates)) return Ok(coordinates)
        }
    }

    val diagonalSquares = moves.filter { (coordinates, _) -> (coordinates.x + coordinates.y) == (board.bounds - 1) }
    if (diagonalSquares.count() == board.bounds - 1) {
        val partialMagicNumberX = diagonalSquares.map { (coordinates, _) -> coordinates.x + 1 }.fold(0) { acc, n -> acc + n }
        val missingX = magicNumber - partialMagicNumberX - 1
        val partialMagicNumberY = diagonalSquares.map { (coordinates, _) -> coordinates.y + 1 }.fold(0) { acc, n -> acc + n }
        val missingY = magicNumber - partialMagicNumberY - 1
        val coordinates = Coordinates(missingX, missingY)
        if (!board.moves.map(MoveRequest::coordinates).contains(coordinates)) return Ok(coordinates)
    }

    val antiDiagonalSquares = moves.filter { (coordinates, _) -> coordinates.x == coordinates.y }
    if (antiDiagonalSquares.count() == board.bounds - 1) {
        val partialMagicNumberX = antiDiagonalSquares.map { (coordinates, _) -> coordinates.x + 1 }.fold(0) { acc, n -> acc + n }
        val missingX = magicNumber - partialMagicNumberX - 1
        val partialMagicNumberY = antiDiagonalSquares.map { (coordinates, _) -> coordinates.y + 1 }.fold(0) { acc, n -> acc + n }
        val missingY = magicNumber - partialMagicNumberY - 1
        val coordinates = Coordinates(missingX, missingY)
        if (!board.moves.map(MoveRequest::coordinates).contains(coordinates)) return Ok(coordinates)
    }

    return Err(Throwable("No winning move"))
}