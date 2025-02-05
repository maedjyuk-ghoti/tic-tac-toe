package com.maedjyukghoti.tictactoe

import com.maedjyukghoti.tictactoe.logic.Coordinates

sealed interface UserIntent {
    data class Move(val coordinates: Coordinates): UserIntent
    data class Undo(val count: Int) : UserIntent
    data class Error(val error: GameError) : UserIntent
    data object Quit : UserIntent
}