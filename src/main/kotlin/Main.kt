import com.github.michaelbull.result.*

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments at Run/Debug configuration
    println("Program arguments: ${args.joinToString()}")

    tictactoe(3)
}

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
 * A board for a game of tic-tac-toe.
 * Assume the board is a square.
 *
 * @param grid is a map containing all moves made and who made them
 * @param bounds is the number of squares in a row/column
 */
data class Board(val grid: Map<Coordinates, Player>, val bounds: Int)

/**
 * A pair of X, Y coordinates.
 *
 * Convenience class to keep in line with a Data/Domain Oriented approach.
 */
data class Coordinates(val x: Int, val y: Int)

/**
 * A move made on a board.
 *
 * @param coordinates the coordinates of the move
 * @param player the player who made the move
 */
data class Move(val coordinates: Coordinates, val player: Player)

/**
 * A request for a move on a board.
 *
 * @param move the move being requested
 * @param board the board on which to play the move
 */
data class MoveRequest(val move: Move, val board: Board)

/**
 * Represents the entire state of the game
 *
 * @param board The current game board
 * @param currentPlayer The player whose turn it is to play
 * @param winner The player who has won the game, [Player.None] indicates there is no winner
 * @param numAvailableMoves The number of available moves. 0 indicates no further moves may be played
 */
data class GameState(val board: Board, val currentPlayer: Player, val winner: Player, val numAvailableMoves: Int)

/** Start a game of tic-tac-toe with a board of size [boardSize] **/
fun tictactoe(boardSize: Int) {
    var gameState = GameState(Board(emptyMap(), boardSize), Player.One, Player.None, boardSize * boardSize)

    while (true) {
        drawBoard(gameState.board)
        if (gameState.winner != Player.None) {
            println("Player ${gameState.winner} wins!")
            break
        }

        if (gameState.numAvailableMoves == 0) {
            println("No more moves available. Game is a tie")
            break
        }

        gameState = turn(gameState)
            .map { checkForWinner(it) to it }
            .fold(
                success = { (winner, updatedBoard) ->
                    GameState(
                        board = updatedBoard,
                        currentPlayer = Player.nextPlayer(gameState.currentPlayer),
                        winner = winner,
                        numAvailableMoves = gameState.numAvailableMoves - 1
                    )
                },
                failure = { throwable ->
                    println(throwable.message)
                    gameState
                }
            )
    }
}

/**
 * Take a turn in the current game state.
 *
 * @return A [Result] with a new [Board] or a [Throwable]
 */
fun turn(gameState: GameState): Result<Board, Throwable> {
    return getInput("Player ${gameState.currentPlayer.number}, enter the square you want to select as `x y`: ")
        .map { Move(it, gameState.currentPlayer) }
        .andThen { validate(MoveRequest(it, gameState.board)) }
        .map { makeMove(it) }
}

/**
 * Display [ask] and retrieve input from the command line.
 *
 * @param ask A string to display on command line before waiting for input
 * @return A [Result] containing the [Coordinates] entered by the [Player] or a [Throwable]
 */
fun getInput(ask: String): Result<Coordinates, Throwable> {
    print(ask)
    val input = readLine() ?: return Err(Throwable("No input received"))

    val split = input.split(" ")
    if (split.size != 2) return Err(Throwable("Input needs to be in the form of `x y` coordinates"))

    val x = split[0].toIntOrNull() ?: return Err(Throwable("x coordinate was not a valid number"))
    val y = split[1].toIntOrNull() ?: return Err(Throwable("y coordinate was not a valid number"))

    return Ok(Coordinates(x, y))
}

/**
 * Checks that a [MoveRequest] is valid
 *
 * @param request The requested move to evaluate
 * @return A [Result] containing a validated [MoveRequest] or a [Throwable]
 */
fun validate(request: MoveRequest): Result<MoveRequest, Throwable> {
    if (request.move.coordinates.x >= request.board.bounds || request.move.coordinates.x < 0) return Err(Throwable("x coordinate is outside the bounds"))
    if (request.move.coordinates.y >= request.board.bounds || request.move.coordinates.y < 0) return Err(Throwable("y coordinate is outside the bounds"))
    if (request.board.grid.containsKey(request.move.coordinates)) return Err(Throwable("That square has already been played"))
    return Ok(request)
}

/** Play the [MoveRequest] and return the new [Board] **/
fun makeMove(request: MoveRequest): Board {
    val newGrid = request.board.grid.plus(request.move.coordinates to request.move.player)
    return Board(newGrid, request.board.bounds)
}

/** Check the [Board] for a winning player. Return [Player.None] if no winner is found. **/
fun checkForWinner(board: Board): Player {
    // check all x
    for (i in 0 until board.bounds) {
        var lastPlayerFound = Player.None
        for (j in 0 until board.bounds) {
            val square = board.grid[Coordinates(i, j)]
            if (square == null) break // square isn't used, can't win on this column
            else if (lastPlayerFound == Player.None) lastPlayerFound = square // 1st used square found
            else if (lastPlayerFound != square) break // a player doesn't own consecutive squares
            else if (lastPlayerFound == square) { // a player owns consecutive squares
                if (j == board.bounds - 1) return lastPlayerFound
                else continue
            }
        }
    }

    // check all y
    for (i in 0 until board.bounds) {
        var lastPlayerFound = Player.None
        for (j in 0 until board.bounds) {
            val square = board.grid[Coordinates(j, i)]
            if (square == null) break // square isn't used, can't win on this row
            else if (lastPlayerFound == Player.None) lastPlayerFound = square // 1st used square found
            else if (lastPlayerFound != square) break // a player doesn't own consecutive squares
            else if (lastPlayerFound == square) { // a player owns consecutive squares
                if (j == board.bounds - 1) return lastPlayerFound
                else continue
            }
        }
    }

    // check diagonals 0,0 -> n,n
    var lastPlayerFound = Player.None
    for (i in 0 until board.bounds) {
        val square = board.grid[Coordinates(i, i)]
        if (square == null) break // square isn't used, can't win on this row
        else if (lastPlayerFound == Player.None) lastPlayerFound = square // 1st used square found
        else if (lastPlayerFound != square) break // a player doesn't own consecutive squares
        else if (lastPlayerFound == square) { // a player owns consecutive squares
            if (i == board.bounds - 1) return lastPlayerFound
            else continue
        }
    }

    // check diagonals 0,n -> n,0
    lastPlayerFound = Player.None
    for (i in 0 until board.bounds) {
        val square = board.grid[Coordinates(i, board.bounds - i - 1)]
        if (square == null) break // square isn't used, can't win on this row
        else if (lastPlayerFound == Player.None) lastPlayerFound = square // 1st used square found
        else if (lastPlayerFound != square) break // a player doesn't own consecutive squares
        else if (lastPlayerFound == square) { // a player owns consecutive squares
            if (i == board.bounds - 1) return lastPlayerFound
            else continue
        }
    }

    return Player.None
}

/**
 * Draw the board to screen
 *  0,0 is the bottom left corner
 *  n,n is the top right corner
 *
 *  todo handle boards where n > 9
 */
fun drawBoard(board: Board) {
    val lastIndex = board.bounds - 1
    val buffer = StringBuilder()

    for (i in lastIndex downTo 0) {
        // draw row number
        buffer.append("$i ")

        for (j in 0..lastIndex) {
            val square = board.grid[Coordinates(j, i)] ?: Player.None
            // draw square and column separator
            when (j) {
                lastIndex -> buffer.append(" ${square.symbol}")
                else -> buffer.append(" ${square.symbol} |")
            }
        }

        // draw row separator
        when (i) {
            0 -> buffer.append("\n")
            else -> {
                buffer.append("\n  ")
                for (k in 0 until ((board.bounds * 4) - 1)) buffer.append('-')
                buffer.append("\n")
            }
        }
    }

    // draw column number
    for (h in 0..lastIndex) {
        buffer.append("   $h")
    }
    buffer.append("\n")

    print(buffer.toString())
}