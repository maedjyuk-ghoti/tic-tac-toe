import com.github.michaelbull.result.*

sealed class Action {
    abstract fun act(gameState: GameState): Result<GameState, Throwable>

    companion object {
        fun getAction(input: String): Action {
            return when (input[0]) {
                'm' -> Move(input.substring(1).trim())
                'u' -> Undo
                else -> Error("Invalid input")
            }
        }
    }
}

class Error(private val reason: String) : Action() {
    override fun act(gameState: GameState): Result<GameState, Throwable> {
        return Err(Throwable(reason))
    }
}

class Move(private val input: String) : Action() {
    override fun act(gameState: GameState): Result<GameState, Throwable> {
        return parse(input)
            .andThen { coordinates -> validate(MoveRequest(coordinates, gameState.currentPlayer, gameState.board.moves.count()), gameState.board) }
            .map { request -> makeMove(request, gameState.board) }
            .map { updatedBoard -> checkForWinner(updatedBoard) to updatedBoard }
            .map { (winner, updatedBoard) ->
                val updateAvailableMoves = gameState.numAvailableMoves - 1
                GameState(updatedBoard, Player.nextPlayer(gameState.currentPlayer), winner, updateAvailableMoves)
            }
    }
}

object Undo : Action() {
    override fun act(gameState: GameState): Result<GameState, Throwable> {
        return undoMove(gameState.board)
            .map { updatedBoard ->
                val updatedAvailableMoves = gameState.numAvailableMoves + 1
                GameState(
                    updatedBoard,
                    Player.previousPlayer(gameState.currentPlayer),
                    gameState.winner,
                    updatedAvailableMoves
                )
            }
    }
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

fun undoMove(board: Board): Result<Board, Throwable> {
    if (board.moves.isEmpty()) return Err(Throwable("No moves to undo"))
    return Ok(Board(board.moves.subList(0, board.moves.lastIndex), board.bounds))
}