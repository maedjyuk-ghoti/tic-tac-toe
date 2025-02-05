package com.maedjyukghoti.tictactoe

import com.maedjyukghoti.tictactoe.logic.Coordinates

sealed interface GameError

data class GameOptionsError(
    val boardSize: Pair<Int, Boolean>,
    val numberOfHumans: Pair<Int, Boolean>,
    val humanPosition: Pair<Int, Boolean>,
    val botLevel: Pair<Int, Boolean>,
) : GameError

sealed class InputError : GameError {
    data object MissingInput : InputError()

    data class InvalidAction(val input: String) : InputError()

    data class InvalidCoordinates(val input: String) : InputError()
}

sealed class MoveError : GameError {
    data object NoAvailableMoves : MoveError()

    data object NoWinningMove : MoveError()

    data class InvalidCoordinates(val coordinates: Coordinates) : MoveError()

    data object CoordinateTaken : MoveError()
}

sealed class UndoError: GameError {
    data object NoMovesToUndo : UndoError()

    data object RequestTooLarge : UndoError()
}