import com.github.michaelbull.result.*
import players.*
import kotlin.math.log10

val actions = """
    |Actions:
    |   Move: m [x y]
    |   Undo: u
    |
""".trimMargin()

fun main(args: Array<String>) {
    println("Welcome to Tic-Tac-Toe!")
    println("Program arguments: ${args.joinToString()}")

    val opts = getopt(args)
    val gameOptions = GameOptions(
        boardSize = opts["--board-size"]?.firstOrNull()?.toInt() ?: 3,
        players = opts["--players"]?.firstOrNull()?.toInt() ?: 2,
        botLevel = opts["--bot-level"]?.firstOrNull()?.toInt() ?: 0,
    )
    tictactoe(gameOptions)
}

fun getopt(args: Array<String>): Map<String, List<String>> {
    return args.fold(mutableListOf()) { acc: MutableList<MutableList<String>>, s: String ->
        acc.apply {
            if (s.startsWith('-')) add(mutableListOf(s))
            else last().add(s)
        }
    }.associate { it[0] to it.drop(1) }
}

/** Start a game of tic-tac-toe **/
fun tictactoe(gameOptions: GameOptions) {
    println("Welcome to TicTacToe")
    println(actions)

    val players = getPlayers(gameOptions.players, gameOptions.botLevel)
    var gameState = GameState(Board(emptyList(), gameOptions.boardSize), PlayerInfo.One, PlayerInfo.None)

    while (true) {
        println(drawBoard(gameState.board))
        if (gameState.winner != PlayerInfo.None) {
            println("Player ${gameState.winner} wins!")
            break
        }

        if (gameState.board.moves.count() == gameState.board.totalMovesAllowed()) {
            println("No more moves available. Game is a tie")
            break
        }

        players.getValue(gameState.currentPlayerInfo)
            .getAction(gameState)
            .andThen { action -> action.act(gameState) }
            .onSuccess { newGameState -> gameState = newGameState }
            .onFailure { throwable ->
                println(throwable.message)
                println(actions)
            }
    }
}

fun getPlayers(numPlayers: Int, botLevel: Int): Map<PlayerInfo, Player> {
    return if (numPlayers == 1) {
        mapOf(
            PlayerInfo.One to HumanPlayer(PlayerInfo.One, ::readLine, ::humanPrompt),
            PlayerInfo.Two to BotPlayer(PlayerInfo.Two, ::botPrompt, getBotAtLevel(botLevel))
        )
    } else {
        mapOf(
            PlayerInfo.One to HumanPlayer(PlayerInfo.One, ::readLine, ::humanPrompt),
            PlayerInfo.Two to HumanPlayer(PlayerInfo.Two, ::readLine, ::humanPrompt)
        )
    }
}

fun getBotAtLevel(botLevel: Int): BotStrategy {
    return when (botLevel) {
        1 -> OneLayerBot
        2 -> TwoLayerBot
        else -> RandomBot
    }
}

fun humanPrompt(name: String) {
    val ask = "Player %s's turn: ".format(name)
    print(ask)
}

fun botPrompt(name: String, move: String) {
    val ask = "Player %s's turn (Bot): %s".format(name, move)
    print(ask)
}

/**
 * Transform the board into a string for display
 *  0,0 is the bottom left corner
 *  n,n is the top right corner
 */
fun drawBoard(board: Board): String {
    val lastIndex = board.bounds - 1
    val buffer = StringBuilder("\n")
    val grid = board.moves.associate { request -> request.coordinates to request.playerInfo }

    for (i in lastIndex downTo 0) {
        // draw row number
        buffer.append(appendWithSpaceAfter(2, i))

        for (j in 0..lastIndex) {
            val square = grid[Coordinates(j, i)] ?: PlayerInfo.None
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