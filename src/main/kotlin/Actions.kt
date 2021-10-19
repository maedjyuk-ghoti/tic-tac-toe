import com.github.michaelbull.result.*

sealed class Action {
    abstract fun act(gameState: GameState): Result<GameState, Throwable>
}

class Move(private val coordinates: Coordinates) : Action() {
    override fun act(gameState: GameState): Result<GameState, Throwable> {
        return validate(MoveRequest(coordinates, gameState.currentPlayerInfo, gameState.board.getNextMoveNumber()), gameState.board)
            .map { request -> makeMove(request, gameState.board) }
            .map { updatedBoard -> updatedBoard.checkForWinner() to updatedBoard }
            .map { (winner, updatedBoard) -> GameState(updatedBoard, PlayerInfo.nextPlayer(gameState.currentPlayerInfo), winner) }
    }
}

object Undo : Action() {
    override fun act(gameState: GameState): Result<GameState, Throwable> {
        return gameState.board.undoMove()
            .map { updatedBoard -> GameState(updatedBoard, PlayerInfo.previousPlayer(gameState.currentPlayerInfo), gameState.winner) }
    }
}

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