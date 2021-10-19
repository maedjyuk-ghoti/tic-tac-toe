@file:Suppress("unused")

package players

import Action
import Board
import Coordinates
import GameState
import Move
import MoveRequest
import PlayerInfo
import Undo
import com.github.michaelbull.result.*
import makeMove
import parse
import kotlin.collections.fold

sealed class Player {
    abstract fun getAction(gameState: GameState): Result<Action, Throwable>
}

class HumanPlayer(
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
            'm' -> parse(input.substring(1).trim()).map { Move(it) }
            'u' -> Ok(Undo)
            else -> Err(Throwable("Invalid input"))
        }
    }
}

class RandomBot(
    private val playerInfo: PlayerInfo,
    private val printOut: (String, String) -> Unit
) : Player() {
    override fun getAction(gameState: GameState): Result<Action, Throwable> {
        return getRandomCoordinates(gameState.board)
            .onSuccess { printOut(playerInfo.name, "m ${it.x} ${it.y}") }
            .map { coordinates -> Move(coordinates) }
    }
}

class OneLayerBot(
    private val playerInfo: PlayerInfo,
    private val printOut: (String, String) -> Unit
) : Player() {
    override fun getAction(gameState: GameState): Result<Action, Throwable> {
        return getWinningCoordinates(gameState.board, playerInfo)
            .orElse { getRandomCoordinates(gameState.board) }
            .onSuccess { printOut(playerInfo.name, "m ${it.x} ${it.y}") }
            .map { coordinates -> Move(coordinates) }
    }
}

/**
 * Because of the blocking move, only works for 2 players.
 * What if the blocking move was evaluated for all players?
 *  1) Make list of all players starting from next player excluding current player
 *  2) first to have a blocking move returns
 *  3) else no blocking move
 */
class TwoLayerBot(
    private val playerInfo: PlayerInfo,
    private val printOut: (String, String) -> Unit
) : Player() {
    override fun getAction(gameState: GameState): Result<Action, Throwable> {
        return getWinningCoordinates(gameState.board, playerInfo)
            .orElse { getBlockingCoordinates(gameState.board, PlayerInfo.nextPlayer(playerInfo)) }
            .orElse { getRandomCoordinates(gameState.board) }
            .onSuccess { printOut(playerInfo.name, "m ${it.x} ${it.y}") }
            .map { coordinates -> Move(coordinates) }
    }
}

class PerfectBot(
    private val playerInfo: PlayerInfo,
    private val printOut: (String, String) -> Unit
) : Player() {
    override fun getAction(gameState: GameState): Result<Action, Throwable> {
        TODO("Not yet implemented")
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