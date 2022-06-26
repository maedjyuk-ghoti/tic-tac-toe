package com.maedjyukghoti.tictactoe.logic

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

data class GameOptions(val boardSize: Int, val numberOfHumans: Int, val humanPosition: Int, val botLevel: Int) {
    companion object {
        fun parse(opts: Map<String, List<String>>): Result<GameOptions, Throwable> {
            val gameOptions = GameOptions(
                boardSize = opts["--board-size"]?.firstOrNull()?.toInt() ?: 3,
                numberOfHumans = opts["--number-of-humans"]?.firstOrNull()?.toInt() ?: 2,
                humanPosition = opts["--human-position"]?.firstOrNull()?.toInt() ?: 0,
                botLevel = opts["--bot-level"]?.firstOrNull()?.toInt() ?: 0,
            )

            if (gameOptions.boardSize < 1) return Err(Throwable("Sorry. Only positive board sizes are supported at the moment."))
            if (gameOptions.numberOfHumans > 2) return Err(Throwable("Sorry. I only supports 2 players at the moment."))
            if (gameOptions.numberOfHumans + gameOptions.humanPosition < 2) return Err(Throwable("Must set player position if less than 2 humans will be playing."))

            return Ok(gameOptions)
        }
    }
}

/**
 * A pair of X, Y coordinates.
 *
 * Convenience class to keep in line with a Data/Domain Oriented approach.
 */
data class Coordinates(val x: Int, val y: Int) {
    companion object {
        /**
         * Parse a string for coordinates
         *
         * @param input A string that may contain usable info for com.maedjyukghoti.tictactoe.tictactoe
         * @return A [Result] containing the [Coordinates] entered by the [PlayerInfo] or a [Throwable]
         */
        fun parse(input: String): Result<Coordinates, InputError> {
            val split = input.split(" ")
            if (split.size != 2) return Err(InputError.InvalidCoordinates(input))

            val x = split[0].toIntOrNull() ?: return Err(InputError.InvalidCoordinates(input))
            val y = split[1].toIntOrNull() ?: return Err(InputError.InvalidCoordinates(input))

            return Ok(Coordinates(x, y))
        }
    }
}

/**
 * Players available for a game.
 *
 * @param symbol A symbol to represent the player on the grid
 */
enum class PlayerInfo(val symbol: Char) {
    None(' '),
    One('X'),
    Two('O');

    companion object {
        fun backUp(playerInfo: PlayerInfo, times: Int): PlayerInfo {
            val index = values().indexOf(playerInfo)
            val previousIndex = index - (times % values().lastIndex)
            val adjustedIndex = if (previousIndex < 1) values().lastIndex + previousIndex else previousIndex
            return values()[adjustedIndex]
        }

        fun previousPlayer(playerInfo: PlayerInfo): PlayerInfo {
            val index = values().indexOf(playerInfo)
            val previousIndex = if (index - 1 < 1) values().lastIndex else index - 1
            return values()[previousIndex]
        }

        /**
         * Given a player, return the next player
         */
        fun nextPlayer(playerInfo: PlayerInfo): PlayerInfo {
            val index = values().indexOf(playerInfo)
            val nextIndex = if (index + 1 > values().lastIndex) 1 else index + 1
            return values()[nextIndex]
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

    fun totalMovesAllowed(): Int {
        return bounds * bounds
    }

    fun undoMove(times: Int = 1): Result<Board, GameError> {
        if (moves.isEmpty()) return Err(UndoError.NoMovesToUndo)
        if (times > moves.count()) return Err(UndoError.RequestTooLarge)
        return Ok(Board(moves.subList(0, moves.count() - times), bounds))
    }

    /** Check the [Board] for a winning player. Return [PlayerInfo.None] if no winner is found. **/
    @Deprecated(message = "Use checkForWinner(board: Board) instead", replaceWith = ReplaceWith(expression = "checkForWinner(this)"))
    fun checkForWinner(): PlayerInfo {
        val grid = moves.associate { request -> request.coordinates to request.playerInfo }

        // check all x
        for (i in 0 until bounds) {
            var lastPlayerInfoFound = PlayerInfo.None
            for (j in 0 until bounds) {
                val square = grid[Coordinates(i, j)]
                if (square == null) break // square isn't used, can't win on this column
                else if (lastPlayerInfoFound == PlayerInfo.None) lastPlayerInfoFound = square // 1st used square found
                else if (lastPlayerInfoFound != square) break // a player doesn't own consecutive squares
                else if (lastPlayerInfoFound == square) { // a player owns consecutive squares
                    if (j == bounds - 1) return lastPlayerInfoFound
                    else continue
                }
            }
        }

        // check all y
        for (i in 0 until bounds) {
            var lastPlayerInfoFound = PlayerInfo.None
            for (j in 0 until bounds) {
                val square = grid[Coordinates(j, i)]
                if (square == null) break // square isn't used, can't win on this row
                else if (lastPlayerInfoFound == PlayerInfo.None) lastPlayerInfoFound = square // 1st used square found
                else if (lastPlayerInfoFound != square) break // a player doesn't own consecutive squares
                else if (lastPlayerInfoFound == square) { // a player owns consecutive squares
                    if (j == bounds - 1) return lastPlayerInfoFound
                    else continue
                }
            }
        }

        // check diagonals 0,0 -> n,n
        var lastPlayerInfoFound = PlayerInfo.None
        for (i in 0 until bounds) {
            val square = grid[Coordinates(i, i)]
            if (square == null) break // square isn't used, can't win on this row
            else if (lastPlayerInfoFound == PlayerInfo.None) lastPlayerInfoFound = square // 1st used square found
            else if (lastPlayerInfoFound != square) break // a player doesn't own consecutive squares
            else if (lastPlayerInfoFound == square) { // a player owns consecutive squares
                if (i == bounds - 1) return lastPlayerInfoFound
                else continue
            }
        }

        // check diagonals 0,n -> n,0
        lastPlayerInfoFound = PlayerInfo.None
        for (i in 0 until bounds) {
            val square = grid[Coordinates(i, bounds - i - 1)]
            if (square == null) break // square isn't used, can't win on this row
            else if (lastPlayerInfoFound == PlayerInfo.None) lastPlayerInfoFound = square // 1st used square found
            else if (lastPlayerInfoFound != square) break // a player doesn't own consecutive squares
            else if (lastPlayerInfoFound == square) { // a player owns consecutive squares
                if (i == bounds - 1) return lastPlayerInfoFound
                else continue
            }
        }

        return PlayerInfo.None
    }
}

/**
 * Represents the entire state of the game
 *
 * @param board The current game board
 * @param currentPlayerInfo The player whose turn it is to play
 * @param winner The player who has won the game, [PlayerInfo.None] indicates there is no winner
 */
data class GameState(val board: Board, val currentPlayerInfo: PlayerInfo, val winner: PlayerInfo)

/** Check the [Board] for a winning player. Return [PlayerInfo.None] if no winner is found. **/
fun checkForWinner(board: Board): PlayerInfo =
    board.moves.groupBy(MoveRequest::playerInfo, MoveRequest::coordinates)    // Group moves based on who played them
        .asSequence()
        .filter { (_, moveSet) -> moveSet.count() >= board.bounds }
        .firstOrNull { (_, moveSet) -> checkForConsecutiveCoordinates(moveSet, board.bounds) }
        ?.component1() ?: PlayerInfo.None

fun checkForConsecutiveCoordinates(moveSet: List<Coordinates>, bounds: Int): Boolean {
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