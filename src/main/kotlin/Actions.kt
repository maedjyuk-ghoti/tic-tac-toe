import com.github.michaelbull.result.*

sealed interface Action {
    fun act(gameState: GameState): Result<GameState, Throwable>

    companion object {
        fun parse(input: String): Result<Action, Throwable> {
            return when (input[0]) {
                'm' -> Coordinates.parse(input.substring(1).trim()).map(::Move)
                'u' -> runCatching { input.substring(1).trim().toInt() }.recover { 1 }.map(::Undo)
                else -> Err(Throwable("Invalid input"))
            }
        }
    }

    class Move(private val coordinates: Coordinates) : Action {
        override fun act(gameState: GameState): Result<GameState, Throwable> {
            return validate(MoveRequest(coordinates, gameState.currentPlayerInfo), gameState.board)
                .map { request -> makeMove(request, gameState.board) }
                .map { updatedBoard -> checkForWinner(updatedBoard) to updatedBoard }
                .map { (winner, updatedBoard) -> GameState(updatedBoard, PlayerInfo.nextPlayer(gameState.currentPlayerInfo), winner) }
        }
    }

    class Undo(val times: Int) : Action {
        override fun act(gameState: GameState): Result<GameState, Throwable> {
            return gameState.board.undoMove(times)
                .map { updatedBoard -> GameState(updatedBoard, PlayerInfo.backUp(gameState.currentPlayerInfo, times), gameState.winner) }
        }
    }
}

/** Return [MoveRequest] if it's valid on [Board] **/
fun validate(request: MoveRequest, board: Board): Result<MoveRequest, Throwable> {
    if (!isValidCoordinate(request.coordinates.x, board.bounds)) return Err(Throwable("x coordinate is outside the bounds"))
    if (!isValidCoordinate(request.coordinates.y, board.bounds)) return Err(Throwable("y coordinate is outside the bounds"))
    if (areCoordinatesTaken(request.coordinates, board.moves)) return Err(Throwable("That square has already been played"))
    return Ok(request)
}

fun isValidCoordinate(coordinate: Int, bounds: Int): Boolean {
    return (coordinate < bounds) && (coordinate >= 0)
}

fun areCoordinatesTaken(coordinates: Coordinates, moves: List<MoveRequest>): Boolean {
    return moves.firstOrNull { moveRequest -> moveRequest.coordinates == coordinates } != null
}

/** Return [Board] after [MoveRequest] is played **/
fun makeMove(request: MoveRequest, board: Board): Board {
    val updatedMoves = board.moves.plus(request)
    return Board(updatedMoves, board.bounds)
}