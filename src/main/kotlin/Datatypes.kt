import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

/**
 * A pair of X, Y coordinates.
 *
 * Convenience class to keep in line with a Data/Domain Oriented approach.
 */
data class Coordinates(val x: Int, val y: Int)

/**
 * Players available for a game.
 *
 * @param number The player's number
 * @param symbol A symbol to represent the player on the grid
 */
enum class Player(val number: Int, val symbol: Char) {
    None(0, ' '),
    One(1, 'X'),
    Two(2, 'O');

    companion object {
        fun previousPlayer(player: Player): Player {
            val index = values().indexOf(player)
            val nextIndex = if (index - 1 > values().lastIndex) 1 else index - 1
            return values()[nextIndex]
        }

        /**
         * Given a player, return the next player
         */
        fun nextPlayer(player: Player): Player {
            val index = values().indexOf(player)
            val nextIndex = if (index + 1 > values().lastIndex) 1 else index + 1
            return values()[nextIndex]
        }
    }
}

/**
 * A move made on a board.
 *
 * @param coordinates the coordinates of the move
 * @param player the player who made the move
 */
data class MoveRequest(val coordinates: Coordinates, val player: Player, val number: Int)

data class Board(val moves: List<MoveRequest>, val bounds: Int) {
    fun getRemainingCoordinates(): List<Coordinates> {
        val playedCoordinates = moves.map(MoveRequest::coordinates)
        return List(bounds * bounds) { index -> Coordinates(index / bounds, index % bounds) }
            .filterNot(playedCoordinates::contains)
    }

    fun totalMovesAllowed(): Int {
        return bounds * bounds
    }

    fun getNextMoveNumber(): Int {
        return moves.count()
    }

    fun undoMove(): Result<Board, Throwable> {
        if (moves.isEmpty()) return Err(Throwable("No moves to undo"))
        return Ok(Board(moves.subList(0, moves.lastIndex), bounds))
    }

    /**
     * Check the [Board] for a winning player. Return [Player.None] if no winner is found.
     * Currently, only works for diagonal wins on an NxN board where N is even.
     *  Fix would be to check the 2 diagonals extra diagonals starting from x = 1
     **/
    fun checkForWinner(): Player {
        val grid = moves.associate { request -> request.coordinates to request.player }

        // check all x
        for (i in 0 until bounds) {
            var lastPlayerFound = Player.None
            for (j in 0 until bounds) {
                val square = grid[Coordinates(i, j)]
                if (square == null) break // square isn't used, can't win on this column
                else if (lastPlayerFound == Player.None) lastPlayerFound = square // 1st used square found
                else if (lastPlayerFound != square) break // a player doesn't own consecutive squares
                else if (lastPlayerFound == square) { // a player owns consecutive squares
                    if (j == bounds - 1) return lastPlayerFound
                    else continue
                }
            }
        }

        // check all y
        for (i in 0 until bounds) {
            var lastPlayerFound = Player.None
            for (j in 0 until bounds) {
                val square = grid[Coordinates(j, i)]
                if (square == null) break // square isn't used, can't win on this row
                else if (lastPlayerFound == Player.None) lastPlayerFound = square // 1st used square found
                else if (lastPlayerFound != square) break // a player doesn't own consecutive squares
                else if (lastPlayerFound == square) { // a player owns consecutive squares
                    if (j == bounds - 1) return lastPlayerFound
                    else continue
                }
            }
        }

        // check diagonals 0,0 -> n,n
        var lastPlayerFound = Player.None
        for (i in 0 until bounds) {
            val square = grid[Coordinates(i, i)]
            if (square == null) break // square isn't used, can't win on this row
            else if (lastPlayerFound == Player.None) lastPlayerFound = square // 1st used square found
            else if (lastPlayerFound != square) break // a player doesn't own consecutive squares
            else if (lastPlayerFound == square) { // a player owns consecutive squares
                if (i == bounds - 1) return lastPlayerFound
                else continue
            }
        }

        // check diagonals 0,n -> n,0
        lastPlayerFound = Player.None
        for (i in 0 until bounds) {
            val square = grid[Coordinates(i, bounds - i - 1)]
            if (square == null) break // square isn't used, can't win on this row
            else if (lastPlayerFound == Player.None) lastPlayerFound = square // 1st used square found
            else if (lastPlayerFound != square) break // a player doesn't own consecutive squares
            else if (lastPlayerFound == square) { // a player owns consecutive squares
                if (i == bounds - 1) return lastPlayerFound
                else continue
            }
        }

        return Player.None
    }
}

/**
 * Represents the entire state of the game
 *
 * @param board The current game board
 * @param currentPlayer The player whose turn it is to play
 * @param winner The player who has won the game, [Player.None] indicates there is no winner
 * @param numAvailableMoves The number of available moves. 0 indicates no further moves may be played
 */
data class GameState(val board: Board, val currentPlayer: Player, val winner: Player)