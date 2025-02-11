package com.maedjyukghoti.tictactoe.logic

import getEmptyBoard
import org.junit.Test
import kotlin.test.assertTrue

internal class MakeMoveTest {
    @Test
    fun `move on board increases move count by 1`() {
        val oldBoard = getEmptyBoard(3)
        val request = MoveRequest(Coordinates(1, 1), PlayerInfo.Two)
        val newBoard = oldBoard.makeMove(request)

        assertTrue(oldBoard.moves.size < newBoard.moves.size, "newBoard should be larger than old board")
        assertTrue(newBoard.moves.containsAll(oldBoard.moves), "oldBoard should be a subset of newBoard")
        assertTrue(newBoard.moves.contains(request), "newBoard should contain the requested coordinates")
    }
}