import com.github.michaelbull.result.*
import kotlin.math.log10

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments at Run/Debug configuration
    println("Program arguments: ${args.joinToString()}")

    tictactoe(15)
}

/** Start a game of tic-tac-toe with a board of size [boardSize] **/
fun tictactoe(boardSize: Int) {
    var gameState = GameState(Board(emptyList(), boardSize), Player.One, Player.None, boardSize * boardSize)

    while (true) {
        print(drawBoard(gameState.board))
        if (gameState.winner != Player.None) {
            println("Player ${gameState.winner} wins!")
            break
        }

        if (gameState.numAvailableMoves == 0) {
            println("No more moves available. Game is a tie")
            break
        }

        turn(gameState, ::print, ::readLine)
            .onSuccess { newGameState -> gameState = newGameState }
            .onFailure { throwable -> println(throwable.message) }
    }
}

/**
 * Take a turn in the current game state.
 *
 * @return A [Result] with a new [Board] or a [Throwable]
 */
fun turn(gameState: GameState, printOut: (String) -> Unit, readIn: () -> String?): Result<GameState, Throwable> {
    val ask = "Player ${gameState.currentPlayer.number}, enter the square you want to select as `x y`: "

    return requestInput(ask, printOut, readIn)
        .andThen { input -> parse(input) }
        .andThen { coordinates -> validate(MoveRequest(coordinates, gameState.currentPlayer, gameState.board.moves.count()), gameState.board) }
        .map { request -> makeMove(request, gameState.board) }
        .map { updatedBoard -> checkForWinner(updatedBoard) to updatedBoard }
        .map { (winner, updatedBoard) ->
            val updateAvailableMoves = gameState.numAvailableMoves - 1
            GameState(updatedBoard, Player.nextPlayer(gameState.currentPlayer), winner, updateAvailableMoves)
        }
}

/**
 * Display [ask] and retrieve input from the command line.
 *
 * @param ask A string to display on command line before waiting for input
 * @param printOut A method to print a string out
 * @param readIn A method that reads a string in
 * @return A [Result] containing the [String] entered by the [Player] or a [Throwable]
 */
fun requestInput(ask: String, printOut: (String) -> Unit, readIn: () -> String?): Result<String, Throwable> {
    printOut(ask)
    val input = readIn() ?: return Err(Throwable("No input received"))
    return Ok(input)
}

/**
 * Parse a string for coordinates
 *
 * @param input A string that may contain usable info for tictactoe
 * @return A [Result] containing the [Coordinates] entered by the [Player] or a [Throwable]
 */
fun parse(input: String): Result<Coordinates, Throwable> {
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
fun validate(request: MoveRequest, board: Board): Result<MoveRequest, Throwable> {
    if (request.coordinates.x >= board.bounds || request.coordinates.x < 0) return Err(Throwable("x coordinate is outside the bounds"))
    if (request.coordinates.y >= board.bounds || request.coordinates.y < 0) return Err(Throwable("y coordinate is outside the bounds"))
    if (board.moves.associateBy(MoveRequest::coordinates).contains(request.coordinates)) return Err(Throwable("That square has already been played"))
    return Ok(request)
}

/** Play the [MoveRequest] and return the new [Board] **/
fun makeMove(request: MoveRequest, board: Board): Board {
    val updatedMoves = board.moves.plus(request)
    return Board(updatedMoves, board.bounds)
}

/** Check the [Board] for a winning player. Return [Player.None] if no winner is found. **/
fun checkForWinner(board: Board): Player {
    val grid = board.moves.associate { request -> request.coordinates to request.player }

    // check all x
    for (i in 0 until board.bounds) {
        var lastPlayerFound = Player.None
        for (j in 0 until board.bounds) {
            val square = grid[Coordinates(i, j)]
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
            val square = grid[Coordinates(j, i)]
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
        val square = grid[Coordinates(i, i)]
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
        val square = grid[Coordinates(i, board.bounds - i - 1)]
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
 * Transform the board into a string for display
 *  0,0 is the bottom left corner
 *  n,n is the top right corner
 *
 *  todo handle boards where n > 9
 */
fun drawBoard(board: Board): String {
    val lastIndex = board.bounds - 1
    val buffer = StringBuilder()
    val grid = board.moves.associate { request -> request.coordinates to request.player }

    for (i in lastIndex downTo 0) {
        // draw row number
        buffer.append(appendWithSpaceAfter(2, i))

        for (j in 0..lastIndex) {
            val square = grid[Coordinates(j, i)] ?: Player.None
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
        buffer.append(appendWithSpaceBefore(4, h))
    }
    buffer.append("\n")

    return buffer.toString()
}

fun appendWithSpaceBefore(spaceCount: Int, number: Int): String {
    // log10(0) is undefined, make a special case for it
    val magnitude = if (number == 0) 1 else (log10(number.toDouble()).toInt() + 1)

    if (spaceCount < magnitude) return ""

    val buffer = StringBuilder()

    for (i in 0 until (spaceCount - magnitude)) buffer.append(' ')
    buffer.append(number)

    return buffer.toString()
}

fun appendWithSpaceAfter(spaceCount: Int, number: Int): String {
    // log10(0) is undefined, make a special case for it
    val magnitude = if (number == 0) 1 else (log10(number.toDouble()).toInt() + 1)

    if (spaceCount < magnitude) return ""

    val buffer = StringBuilder()

    buffer.append(number)
    for (i in 0 until (spaceCount - magnitude)) buffer.append(' ')

    return buffer.toString()
}

fun undoMove(board: Board): Result<Board, Throwable> {
    if (board.moves.isEmpty()) return Err(Throwable("No moves to undo"))
    return Ok(Board(board.moves.subList(0, board.moves.lastIndex), board.bounds))
}