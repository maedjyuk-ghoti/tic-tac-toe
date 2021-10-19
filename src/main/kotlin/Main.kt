import com.github.michaelbull.result.*
import kotlin.math.log10

fun getopt(args: Array<String>): Map<String, List<String>> =
    args.fold(mutableListOf()) { acc: MutableList<MutableList<String>>, s: String ->
        acc.apply {
            if (s.startsWith('-')) add(mutableListOf(s))
            else last().add(s)
        }
    }.associate { it[0] to it.drop(1) }

data class GameOptions(
    val boardSize: Int,
    val players: Int,
//    val uesBots: Boolean,
//    val botLevel: Int,
//    val botPlayerPosition: Int
)

fun main(args: Array<String>) {
    println("Welcome to Tic-Tac-Toe!")

    // Try adding program arguments at Run/Debug configuration
    println("Program arguments: ${args.joinToString()}")

    val opts = getopt(args)
    val gameOptions = GameOptions(
        boardSize = opts["--board-size"]?.firstOrNull()?.toInt() ?: 3,
        players = opts["--players"]?.firstOrNull()?.toInt() ?: 2
    )
    tictactoe(gameOptions)
}

val actions = """
    |Actions:
    |   Move: m [x y]
    |   Undo: u
    |
""".trimMargin()

/** Start a game of tic-tac-toe **/
fun tictactoe(gameOptions: GameOptions) {
    var gameState = GameState(Board(emptyList(), gameOptions.boardSize), Player.One, Player.None)
    println("Welcome to TicTacToe")
    println(actions)

    while (true) {
        println(drawBoard(gameState.board))
        if (gameState.winner != Player.None) {
            println("Player ${gameState.winner} wins!")
            break
        }

        if (gameState.board.moves.count() == gameState.board.totalMovesAllowed()) {
            println("No more moves available. Game is a tie")
            break
        }

        turn(gameState, ::print, ::readLine)
            .onSuccess { newGameState -> gameState = newGameState }
            .onFailure { throwable ->
                println(throwable.message)
                println(actions)
            }
    }
}

/**
 * Transform the board into a string for display
 *  0,0 is the bottom left corner
 *  n,n is the top right corner
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

/**
 * Take a turn in the current game state.
 *
 * @return A [Result] with a new [Board] or a [Throwable]
 */
fun turn(gameState: GameState, printOut: (String) -> Unit, readIn: () -> String?): Result<GameState, Throwable> {
    val ask = "Player %s's turn: ".format(gameState.currentPlayer.number)

    return requestInput(ask, printOut, readIn)
        .map { input -> Action.getAction(input) }
        .andThen { action -> action.act(gameState) }
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