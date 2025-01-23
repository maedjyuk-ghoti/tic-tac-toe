package com.maedjyukghoti.tictactoe.logic

import com.github.michaelbull.result.*

sealed interface Action {
    fun act(gameState: GameState): Result<GameState, GameError>

    companion object {
        fun parse(input: String): Result<Action, InputError> {
            return when (input[0]) {
                'm' -> Coordinates.parse(input.substring(1).trim()).map(::Move)
                'u' -> runCatching { input.substring(1).trim().toInt() }.recover { 1 }.map(::Undo)
                else -> Err(InputError.InvalidAction(input))
            }
        }
    }

    class Move(val coordinates: Coordinates) : Action {
        override fun act(gameState: GameState): Result<GameState, GameError> {
            return validate(MoveRequest(coordinates, gameState.currentPlayerInfo), gameState.board)
                .map { request -> makeMove(request, gameState.board) }
                .map { updatedBoard -> checkForWinner(updatedBoard) to updatedBoard }
                .map { (winner, updatedBoard) -> GameState(updatedBoard, gameState.players, PlayerInfo.nextPlayer(gameState.currentPlayerInfo), winner, null) }
        }
    }

    class Undo(val times: Int) : Action {
        override fun act(gameState: GameState): Result<GameState, GameError> {
            return gameState.board.undoMove(times)
                .map { updatedBoard -> GameState(updatedBoard, gameState.players, PlayerInfo.backUp(gameState.currentPlayerInfo, times), gameState.winner, null) }
        }
    }
}

/** Return [MoveRequest] if it's valid on [Board] **/
fun validate(request: MoveRequest, board: Board): Result<MoveRequest, MoveError> {
    if (!isValidCoordinate(request.coordinates.x, board.bounds)) return Err(MoveError.InvalidCoordinates(request.coordinates))
    if (!isValidCoordinate(request.coordinates.y, board.bounds)) return Err(MoveError.InvalidCoordinates(request.coordinates))
    if (areCoordinatesTaken(request.coordinates, board.moves)) return Err(MoveError.CoordinateTaken)
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