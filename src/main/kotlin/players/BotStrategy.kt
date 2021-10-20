package players

import Board
import Coordinates
import GameState
import MoveRequest
import PlayerInfo
import com.github.michaelbull.result.*
import makeMove
import kotlin.collections.fold

sealed interface BotStrategy {
    fun getCoordinates(gameState: GameState): Result<Coordinates, Throwable>

    companion object {
        fun getBotAtLevel(botLevel: Int): BotStrategy {
            return when (botLevel) {
                1 -> OneLayer
                2 -> TwoLayer
                3 -> MiniMax
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

    object MiniMax : BotStrategy {
        override fun getCoordinates(gameState: GameState): Result<Coordinates, Throwable> {
            val choice = minimax(gameState.board, gameState.currentPlayerInfo, gameState.currentPlayerInfo, true, 0)
            return Ok(choice.option.coordinates)
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
                val moveRequest = MoveRequest(coordinates, playerInfo)
                val newBoard = makeMove(moveRequest, board)
                val winner = newBoard.checkForWinner()
                winner == playerInfo
            }
    }.mapError { Throwable("No winning move") }
}

data class Choice<S, T>(val option: S, val value: T, val depth: Int)

fun minimax(board: Board, myPlayer: PlayerInfo, currentPlayer: PlayerInfo, isMax: Boolean, depth: Int): Choice<MoveRequest, Int> {
    val winner = board.checkForWinner()
    if (winner == myPlayer) return Choice(board.moves.last(), 10 - depth, depth)
    if (winner == PlayerInfo.nextPlayer(myPlayer)) return Choice(board.moves.last(), -10 + depth, depth)
    if (board.moves.count() == (board.bounds * board.bounds)) return Choice(board.moves.last(), 0, depth)

    fun comparison(a: Choice<MoveRequest, Int>, b: Choice<MoveRequest, Int>) : Choice<MoveRequest, Int> {
        return if (isMax) {
            if (a.value > b.value) a else b
        } else {
            if (a.value < b.value) a else b
        }
    }

    val choices = board.getRemainingCoordinates()
        .map { coordinates ->
            val moveRequest = MoveRequest(coordinates, currentPlayer)
            val updateBoard = makeMove(moveRequest, board)
            minimax(updateBoard, myPlayer, PlayerInfo.nextPlayer(currentPlayer), !isMax, depth + 1)
        }

    return choices.reduce { acc, choice -> comparison(acc, choice) }
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