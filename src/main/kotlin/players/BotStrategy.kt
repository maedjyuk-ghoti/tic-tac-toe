package players

import Board
import Coordinates
import GameState
import MoveError
import MoveRequest
import PlayerInfo
import checkForWinner
import com.github.michaelbull.result.*
import makeMove

sealed interface BotStrategy {
    fun getCoordinates(gameState: GameState, botPlayer: PlayerInfo): Result<Coordinates, MoveError>

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
        override fun getCoordinates(gameState: GameState, botPlayer: PlayerInfo): Result<Coordinates, MoveError> {
            return getRandomCoordinates(gameState.board)
        }
    }

    object OneLayer : BotStrategy {
        override fun getCoordinates(gameState: GameState, botPlayer: PlayerInfo): Result<Coordinates, MoveError> {
            return getWinningCoordinates(gameState.board, botPlayer)
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
        override fun getCoordinates(gameState: GameState, botPlayer: PlayerInfo): Result<Coordinates, MoveError> {
            return getWinningCoordinates(gameState.board, botPlayer)
                .orElse { getBlockingCoordinates(gameState.board, PlayerInfo.nextPlayer(botPlayer)) }
                .orElse { getRandomCoordinates(gameState.board) }
        }
    }

    object MiniMax : BotStrategy {
        private val memo: MutableMap<Set<MoveRequest>, Choice> = mutableMapOf()
        override fun getCoordinates(gameState: GameState, botPlayer: PlayerInfo): Result<Coordinates, MoveError> {
            val choice = minimax(gameState.board, botPlayer, gameState.currentPlayerInfo, 0, memo)
            return Ok(choice.option)
        }
    }
}

fun getRandomCoordinates(board: Board): Result<Coordinates, MoveError> {
    return runCatching { board.getRemainingCoordinates().random() }
        .mapError { MoveError.NoAvailableMoves }
}

/** Return the [Coordinates] to block [PlayerInfo] if they exist **/
fun getBlockingCoordinates(board: Board, playerInfo: PlayerInfo): Result<Coordinates, MoveError> {
    return getWinningCoordinates(board, playerInfo)
}

/** Return the winning [Coordinates] for [PlayerInfo] on the [Board] if they exist. **/
fun getWinningCoordinates(board: Board, playerInfo: PlayerInfo): Result<Coordinates, MoveError> {
    return runCatching {
        board.getRemainingCoordinates()
            .first { coordinates ->
                val moveRequest = MoveRequest(coordinates, playerInfo)
                val newBoard = makeMove(moveRequest, board)
                val winner = checkForWinner(newBoard)
                winner == playerInfo
            }
    }.mapError { MoveError.NoWinningMove }
}

data class Choice(val option: Coordinates, val value: Int, val depth: Int) : Comparable<Choice> {
    override fun compareTo(other: Choice) : Int {
        return if (value > other.value) 1
        else if (value < other.value) -1
        else 0
    }
}

fun minimax(board: Board, myPlayer: PlayerInfo, currentPlayer: PlayerInfo, depth: Int, memo: MutableMap<Set<MoveRequest>, Choice>): Choice {
    val winner = checkForWinner(board)
    if (winner == myPlayer) return Choice(board.moves.last().coordinates, (board.totalMovesAllowed() + 1) - depth, depth)
    else if (winner != PlayerInfo.None) return Choice(board.moves.last().coordinates, ((board.totalMovesAllowed() + 1) * -1) + depth, depth)
    else if (board.moves.count() == board.totalMovesAllowed()) return Choice(board.moves.last().coordinates, 0, depth)

    val choice = if (memo.contains(board.moves.toSet())) memo.getValue(board.moves.toSet())
    else {
        val compare: (Choice, Choice) -> Choice = if (myPlayer == currentPlayer) ::maxOf else ::minOf
        board.getRemainingCoordinates()
            .map { coordinates ->
                val moveRequest = MoveRequest(coordinates, currentPlayer)
                val updateBoard = makeMove(moveRequest, board)
                val nextPlayer = PlayerInfo.nextPlayer(currentPlayer)
                val result = minimax(updateBoard, myPlayer, nextPlayer, depth + 1, memo)
                result.copy(option = updateBoard.moves.last().coordinates)
            }.reduce(compare)
    }

    memo[board.moves.toSet()] = choice
    return choice
}

fun alphabeta(board: Board, maximizingPlayerInfo: PlayerInfo, currentPlayer: PlayerInfo, depth: Int, alpha: Int, beta: Int): Choice {
    if (depth == 0) return Choice(board.moves.last().coordinates, depth, depth)

    val winner = checkForWinner(board)
    if (winner == maximizingPlayerInfo) return Choice(board.moves.last().coordinates, depth, depth)
    else if (winner != PlayerInfo.None) return Choice(board.moves.last().coordinates, depth, depth)
    else if (board.moves.count() == board.totalMovesAllowed()) return Choice(board.moves.last().coordinates, 0, depth)

    if (maximizingPlayerInfo == currentPlayer) {
        val choice = board.getRemainingCoordinates()
            .map { coordinates ->
                val moveRequest = MoveRequest(coordinates, currentPlayer)
                val updateBoard = makeMove(moveRequest, board)
                val nextPlayer = PlayerInfo.nextPlayer(currentPlayer)
                val result = alphabeta(updateBoard, maximizingPlayerInfo, nextPlayer, depth - 1, alpha, beta)
                result.copy(option = updateBoard.moves.last().coordinates)
            }.filter { it.value < beta }
            .reduce(::maxOf)

        return choice.copy(value = maxOf(choice.value, alpha))
    } else {
        val choice = board.getRemainingCoordinates()
            .map { coordinates ->
                val moveRequest = MoveRequest(coordinates, currentPlayer)
                val updateBoard = makeMove(moveRequest, board)
                val nextPlayer = PlayerInfo.nextPlayer(currentPlayer)
                val result = alphabeta(updateBoard, maximizingPlayerInfo, nextPlayer, depth - 1, alpha, beta)
                result.copy(option = updateBoard.moves.last().coordinates)
            }.filter { it.value > alpha }
            .reduce(::maxOf)

        return choice.copy(value = minOf(choice.value, beta))
    }
}