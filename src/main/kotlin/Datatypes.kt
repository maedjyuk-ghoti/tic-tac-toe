import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

data class GameOptions(val boardSize: Int, val players: Int, val botLevel: Int)

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
         * @param input A string that may contain usable info for tictactoe
         * @return A [Result] containing the [Coordinates] entered by the [PlayerInfo] or a [Throwable]
         */
        fun parse(input: String): Result<Coordinates, Throwable> {
            val split = input.split(" ")
            if (split.size != 2) return Err(Throwable("Input needs to be in the form of `x y` coordinates"))

            val x = split[0].toIntOrNull() ?: return Err(Throwable("x coordinate was not a valid number"))
            val y = split[1].toIntOrNull() ?: return Err(Throwable("y coordinate was not a valid number"))

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

    fun undoMove(): Result<Board, Throwable> {
        if (moves.isEmpty()) return Err(Throwable("No moves to undo"))
        return Ok(Board(moves.subList(0, moves.lastIndex), bounds))
    }

    /**
     * Check the [Board] for a winning player. Return [PlayerInfo.None] if no winner is found.
     * Currently, only works for diagonal wins on an NxN board where N is even.
     *  Fix would be to check the 2 diagonals extra diagonals starting from x = 1
     **/
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