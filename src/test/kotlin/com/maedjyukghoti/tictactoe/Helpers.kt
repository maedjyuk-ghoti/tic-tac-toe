package com.maedjyukghoti.tictactoe

import com.maedjyukghoti.tictactoe.logic.Board

fun getEmptyBoard(size: Int): Board {
    return Board(emptyList(), size)
}
