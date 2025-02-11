package com.maedjyukghoti.tictactoe.logic

import getEmptyBoard
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class Undo {
    @Test
    fun `undoing empty game returns error`() {
        val result = getEmptyBoard(3).undoMove()
        assertNotNull(result.component2(), "Nothing to undo")
    }

    @Test
    fun `undoing valid board returns new board`() {
        val board = Board(listOf(MoveRequest(Coordinates(0, 0), PlayerInfo.One)), 3)
        val result = board.undoMove()
        assertNotNull(result.component1(), "Should be a valid undo")
        val newBoard = result.component1()!!
        assertEquals(newBoard.moves.count(), board.moves.count() - 1, "New board should have 1 fewer moves")
        assertTrue(board.moves.containsAll(newBoard.moves))
    }
}
