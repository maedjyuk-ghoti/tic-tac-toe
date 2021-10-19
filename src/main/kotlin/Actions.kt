import com.github.michaelbull.result.*

sealed class Action {
    abstract fun act(gameState: GameState): Result<GameState, Throwable>

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