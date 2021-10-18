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

data class Board(val moves: List<MoveRequest>, val bounds: Int)

/**
 * Represents the entire state of the game
 *
 * @param board The current game board
 * @param currentPlayer The player whose turn it is to play
 * @param winner The player who has won the game, [Player.None] indicates there is no winner
 * @param numAvailableMoves The number of available moves. 0 indicates no further moves may be played
 */
data class GameState(val board: Board, val currentPlayer: Player, val winner: Player, val numAvailableMoves: Int)