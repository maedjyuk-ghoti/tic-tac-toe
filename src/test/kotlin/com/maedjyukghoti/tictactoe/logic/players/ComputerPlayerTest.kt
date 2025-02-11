package com.maedjyukghoti.tictactoe.logic.players

import com.maedjyukghoti.tictactoe.logic.Board
import com.maedjyukghoti.tictactoe.logic.Coordinates
import com.maedjyukghoti.tictactoe.logic.MoveRequest
import com.maedjyukghoti.tictactoe.logic.PlayerInfo
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class ComputerPlayerTest {
    @Test
    fun `no winning move, empty board`() {
        val board = Board(emptyList(), 3)
        val result = getWinningCoordinates(board, PlayerInfo.One)
        assertNotNull(result.component2())
    }

    @Test
    fun `no winning move`() {
        val moves =
            listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(1, 2), PlayerInfo.One),
                MoveRequest(Coordinates(2, 1), PlayerInfo.One),
            )
        val board = Board(moves, 3)
        val result = getWinningCoordinates(board, PlayerInfo.One)
        assertNotNull(result.component2())
    }

    @Test
    fun `winning move row`() {
        val moves =
            listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(1, 0), PlayerInfo.One),
            )
        val board = Board(moves, 3)
        val result = getWinningCoordinates(board, PlayerInfo.One)
        assertNotNull(result.component1())
        assertEquals(result.component1(), Coordinates(2, 0))
    }

    @Test
    fun `winning move column`() {
        val moves =
            listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(0, 2), PlayerInfo.One),
            )
        val board = Board(moves, 3)
        val result = getWinningCoordinates(board, PlayerInfo.One)
        assertNotNull(result.component1())
        assertEquals(result.component1(), Coordinates(0, 1))
    }

    @Test
    fun `winning move diagonal`() {
        val moves =
            listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(1, 1), PlayerInfo.One),
            )
        val board = Board(moves, 3)
        val result = getWinningCoordinates(board, PlayerInfo.One)
        assertNotNull(result.component1())
        assertEquals(result.component1(), Coordinates(2, 2))
    }

    @Test
    fun `winning move anti-diagonal`() {
        val moves =
            listOf(
                MoveRequest(Coordinates(0, 2), PlayerInfo.One),
                MoveRequest(Coordinates(2, 0), PlayerInfo.One),
            )
        val board = Board(moves, 3)
        val result = getWinningCoordinates(board, PlayerInfo.One)
        assertNotNull(result.component1())
        assertEquals(result.component1(), Coordinates(1, 1))
    }
}
