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