package players

import Board
import Coordinates
import MoveRequest
import Player
import com.github.michaelbull.result.*
import makeMove
import kotlin.collections.fold

sealed class ComputerPlayer {
    abstract fun getNextMove(board: Board): Result<Coordinates, Throwable>
}

object RandomPlayer : ComputerPlayer() {
    override fun getNextMove(board: Board): Result<Coordinates, Throwable> {
        return getRandomCoordinates(board)
    }
}

class OneLayerPlayer(private val player: Player) : ComputerPlayer() {
    override fun getNextMove(board: Board): Result<Coordinates, Throwable> {
        return getWinningCoordinates(board, player)
            .orElse { getRandomCoordinates(board) }
    }
}

/**
 * Because of the blocking move, only works for 2 players.
 * What if the blocking move was evaluated for all players?
 *  1) Make list of all players starting from next player excluding current player
 *  2) first to have a blocking move returns
 *  3) else no blocking move
 */
class TwoLayerPlayer(private val player: Player) : ComputerPlayer() {
    override fun getNextMove(board: Board): Result<Coordinates, Throwable> {
        return getWinningCoordinates(board, player)
            .orElse { getBlockingCoordinates(board, Player.nextPlayer(player)) }
            .orElse { getRandomCoordinates(board) }
    }

}

class PerfectPlayer(private val player: Player) : ComputerPlayer() {
    override fun getNextMove(board: Board): Result<Coordinates, Throwable> {
        TODO("Not yet implemented")
    }
}

fun getRandomCoordinates(board: Board): Result<Coordinates, Throwable> {
    return runCatching { board.getRemainingCoordinates().random() }
        .mapError { Throwable("No available moves") }
}

/**
 * Returns the [Coordinates] to block [Player] if they exist
 */
fun getBlockingCoordinates(board: Board, player: Player): Result<Coordinates, Throwable> {
    return getWinningCoordinates(board, player)
}

/**
 * Return the winning [Coordinates] for [Player] if they exist.
 */
fun getWinningCoordinates(board: Board, player: Player): Result<Coordinates, Throwable> {
    return runCatching {
        board.getRemainingCoordinates()
            .first { coordinates ->
                val moveRequest = MoveRequest(coordinates, player, board.moves.count())
                val newBoard = makeMove(moveRequest, board)
                val winner = newBoard.checkForWinner()
                winner == player
            }
        }.mapError { Throwable("No winning move") }
}

// TODO rewrite check for winner with this setup. Looks much easier to understand.
fun getWinningCoordinates2(board: Board, player: Player): Result<Coordinates, Throwable> {
    val myMoves = board.moves.filter { request -> request.player == player }
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