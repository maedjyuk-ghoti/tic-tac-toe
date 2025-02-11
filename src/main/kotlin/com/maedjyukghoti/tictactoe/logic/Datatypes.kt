package com.maedjyukghoti.tictactoe.logic

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.maedjyukghoti.tictactoe.GameError
import com.maedjyukghoti.tictactoe.MoveError
import com.maedjyukghoti.tictactoe.UndoError

data class GameOptions(val boardSize: Int, val numberOfHumans: Int, val humanPosition: Int, val botLevel: Int) {
    companion object {
        fun parse(opts: Map<String, List<String>>): Result<GameOptions, InvalidGameOptions> =
            validateGameOptions(
                boardSize = opts["--board-size"]?.firstOrNull()?.toInt() ?: 3,
                numberOfHumans = opts["--number-of-humans"]?.firstOrNull()?.toInt() ?: 2,
                humanPosition = opts["--human-position"]?.firstOrNull()?.toInt() ?: 0,
                botLevel = opts["--bot-level"]?.firstOrNull()?.toInt() ?: 0,
            )

        private fun validateGameOptions(
            boardSize: Int,
            numberOfHumans: Int,
            humanPosition: Int,
            botLevel: Int,
        ): Result<GameOptions, InvalidGameOptions> {
            val isBoardSizeInvalid = boardSize < 1
            val isNumberOfHumansInvalid = numberOfHumans > 2
            val isHumanPositionInvalid = numberOfHumans + humanPosition < 2
            val isBotLevelInvalid = botLevel in 1..3

            return if (isBoardSizeInvalid || isNumberOfHumansInvalid || isHumanPositionInvalid || isBotLevelInvalid) {
                Err(InvalidGameOptions(isBoardSizeInvalid, isNumberOfHumansInvalid, isHumanPositionInvalid, isBotLevelInvalid))
            } else {
                Ok(GameOptions(boardSize, numberOfHumans, humanPosition, botLevel))
            }
        }
    }
}

data class InvalidGameOptions(
    val isBoardSizeInvalid: Boolean,
    val isNumberOfHumansInvalid: Boolean,
    val isHumanPositionInvalid: Boolean,
    val isBotLevelInvalid: Boolean,
)

/**
 * A pair of X, Y coordinates.
 *
 * Convenience class to keep in line with a Data/Domain Oriented approach.
 */
data class Coordinates(val x: Int, val y: Int)

/**
 * Players available for a game.
 */
enum class PlayerInfo {
    None,
    One,
    Two,
    ;

    companion object {
        fun backUp(
            playerInfo: PlayerInfo,
            times: Int,
        ): PlayerInfo {
            val index = entries.indexOf(playerInfo)
            val previousIndex = index - (times % entries.toTypedArray().lastIndex)
            val adjustedIndex = if (previousIndex < 1) entries.toTypedArray().lastIndex + previousIndex else previousIndex
            return entries[adjustedIndex]
        }

        /**
         * Given a player, return the next player
         */
        fun nextPlayer(playerInfo: PlayerInfo): PlayerInfo {
            val index = entries.indexOf(playerInfo)
            val nextIndex = if (index + 1 > entries.toTypedArray().lastIndex) 1 else index + 1
            return entries[nextIndex]
        }
    }
}

/**
 * A move made on a board.
 *
 * @param coordinates the coordinates of the move
 * @param playerInfo the player who made the move
 */
data class MoveRequest(val coordinates: Coordinates, val playerInfo: PlayerInfo)

data class Board(val moves: List<MoveRequest>, val bounds: Int) {
    fun getRemainingCoordinates(): List<Coordinates> {
        val playedCoordinates = moves.map(MoveRequest::coordinates)
        return List(bounds * bounds) { index -> Coordinates(index / bounds, index % bounds) }
            .filterNot(playedCoordinates::contains)
    }

    fun totalMovesAllowed(): Int = bounds * bounds

    fun undoMove(times: Int = 1): Result<Board, GameError> =
        if (moves.isEmpty()) {
            Err(UndoError.NoMovesToUndo)
        } else if (times > moves.count()) {
            Err(UndoError.RequestTooLarge)
        } else {
            Ok(Board(moves.subList(0, moves.count() - times), bounds))
        }

    /** Return a new [Board] where [MoveRequest] is played **/
    fun makeMove(request: MoveRequest): Board = copy(moves = moves.plus(request))

    /** Return [MoveRequest] if it's valid on [Board] **/
    fun validate(request: MoveRequest): Result<MoveRequest, MoveError> =
        if (!isValidCoordinate(request.coordinates.x, bounds)) {
            Err(MoveError.InvalidCoordinates(request.coordinates))
        } else if (!isValidCoordinate(request.coordinates.y, bounds)) {
            Err(MoveError.InvalidCoordinates(request.coordinates))
        } else if (areCoordinatesTaken(request.coordinates, moves)) {
            Err(MoveError.CoordinateTaken)
        } else {
            Ok(request)
        }

    private fun isValidCoordinate(
        coordinate: Int,
        bounds: Int,
    ): Boolean = (coordinate < bounds) && (coordinate >= 0)

    private fun areCoordinatesTaken(
        coordinates: Coordinates,
        moves: List<MoveRequest>,
    ): Boolean = moves.firstOrNull { moveRequest -> moveRequest.coordinates == coordinates } != null

    /** Check the [Board] for a winning player. Return [PlayerInfo.None] if no winner is found. **/
    @Deprecated(message = "Use checkForWinner(board: Board) instead", replaceWith = ReplaceWith(expression = "checkForWinner(this)"))
    fun checkForWinner(): PlayerInfo {
        val grid = moves.associate { request -> request.coordinates to request.playerInfo }

        // check all x
        for (i in 0 until bounds) {
            var lastPlayerInfoFound = PlayerInfo.None
            for (j in 0 until bounds) {
                val square = grid[Coordinates(i, j)]
                if (square == null) {
                    break // square isn't used, can't win on this column
                } else if (lastPlayerInfoFound == PlayerInfo.None) {
                    lastPlayerInfoFound = square // 1st used square found
                } else if (lastPlayerInfoFound != square) {
                    break // a player doesn't own consecutive squares
                } else /* if (lastPlayerInfoFound == square) */ { // a player owns consecutive squares
                    if (j == bounds - 1) {
                        return lastPlayerInfoFound
                    } else {
                        continue
                    }
                }
            }
        }

        // check all y
        for (i in 0 until bounds) {
            var lastPlayerInfoFound = PlayerInfo.None
            for (j in 0 until bounds) {
                val square = grid[Coordinates(j, i)]
                if (square == null) {
                    break // square isn't used, can't win on this row
                } else if (lastPlayerInfoFound == PlayerInfo.None) {
                    lastPlayerInfoFound = square // 1st used square found
                } else if (lastPlayerInfoFound != square) {
                    break // a player doesn't own consecutive squares
                } else /* if (lastPlayerInfoFound == square) */ { // a player owns consecutive squares
                    if (j == bounds - 1) {
                        return lastPlayerInfoFound
                    } else {
                        continue
                    }
                }
            }
        }

        // check diagonals 0,0 -> n,n
        var lastPlayerInfoFound = PlayerInfo.None
        for (i in 0 until bounds) {
            val square = grid[Coordinates(i, i)]
            if (square == null) {
                break // square isn't used, can't win on this row
            } else if (lastPlayerInfoFound == PlayerInfo.None) {
                lastPlayerInfoFound = square // 1st used square found
            } else if (lastPlayerInfoFound != square) {
                break // a player doesn't own consecutive squares
            } else /* if (lastPlayerInfoFound == square) */ { // a player owns consecutive squares
                if (i == bounds - 1) {
                    return lastPlayerInfoFound
                } else {
                    continue
                }
            }
        }

        // check diagonals 0,n -> n,0
        lastPlayerInfoFound = PlayerInfo.None
        for (i in 0 until bounds) {
            val square = grid[Coordinates(i, bounds - i - 1)]
            if (square == null) {
                break // square isn't used, can't win on this row
            } else if (lastPlayerInfoFound == PlayerInfo.None) {
                lastPlayerInfoFound = square // 1st used square found
            } else if (lastPlayerInfoFound != square) {
                break // a player doesn't own consecutive squares
            } else /* if (lastPlayerInfoFound == square) */ { // a player owns consecutive squares
                if (i == bounds - 1) {
                    return lastPlayerInfoFound
                } else {
                    continue
                }
            }
        }

        return PlayerInfo.None
    }
}

/** Check the [Board] for a winning player. Return [PlayerInfo.None] if no winner is found. **/
fun checkForWinner(board: Board): PlayerInfo =
    board.moves.groupBy(MoveRequest::playerInfo, MoveRequest::coordinates) // Group moves based on who played them
        .asSequence()
        .filter { (_, moveSet) -> moveSet.count() >= board.bounds }
        .firstOrNull { (_, moveSet) -> checkForConsecutiveCoordinates(moveSet, board.bounds) }
        ?.component1() ?: PlayerInfo.None

private fun checkForConsecutiveCoordinates(
    moveSet: List<Coordinates>,
    bounds: Int,
): Boolean {
    // Not enough moves to win
    if (moveSet.count() < bounds) return false

    // Using a list as a 'functional' for loop, e.g. for (i in 0 until board.bounds) ...
    // 1) sort into coordinates on a row
    // 2) count number of coordinates on each row
    // 3) if that count is 3, on a 3x3, they win
    val isWinOnRow = List(bounds) { index -> moveSet.filter { (x, _) -> x == index } }.map(List<Any>::count).contains(bounds)
    // same as row
    val isWinOnColumn = List(bounds) { index -> moveSet.filter { (_, y) -> y == index } }.map(List<Any>::count).contains(bounds)
    // There is only one (anti)diagonal, no need to loop
    // (0,0) -> (n,n)
    val isWinOnDiagonal = moveSet.count { (x, y) -> x == y } == bounds
    // (0,n) -> (n,0), add 1 in the comparison to account for array indexes
    val isWinOnAntiDiagonal = moveSet.count { (x, y) -> (x + y + 1) == bounds } == bounds

    return isWinOnRow || isWinOnColumn || isWinOnDiagonal || isWinOnAntiDiagonal
}
