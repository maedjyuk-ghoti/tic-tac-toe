package players

import Board
import Coordinates
import MoveRequest
import PlayerInfo
import org.junit.Test
import kotlin.test.assertEquals

internal class MinimaxTest {

    @Test
    fun `identify winning move on won board`() {
        val board = Board(
            listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(2, 0), PlayerInfo.Two),
                MoveRequest(Coordinates(0, 1), PlayerInfo.One),
                MoveRequest(Coordinates(2, 1), PlayerInfo.Two),
                MoveRequest(Coordinates(0, 2), PlayerInfo.One),
                MoveRequest(Coordinates(2, 2), PlayerInfo.Two),
                MoveRequest(Coordinates(0, 3), PlayerInfo.One),
            ), 4
        )

        val actual = minimax(board, PlayerInfo.One, PlayerInfo.Two, 0, mutableMapOf())
        val expected = board.moves.last().coordinates
        assertEquals(expected, actual.option, "Should return last move of a won board")
    }

    @Test
    fun `identify winning choice 1 move away`() {
        val board = Board(
            listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(2, 0), PlayerInfo.Two),
                MoveRequest(Coordinates(0, 1), PlayerInfo.One),
                MoveRequest(Coordinates(2, 1), PlayerInfo.Two),
                MoveRequest(Coordinates(0, 2), PlayerInfo.One),
                MoveRequest(Coordinates(2, 2), PlayerInfo.Two),
            ), 4
        )

        val actual = minimax(board, PlayerInfo.One, PlayerInfo.One, 0, mutableMapOf())
        val expected = Coordinates(0, 3)
        assertEquals(expected, actual.option, "Best move possible is the winning move")
    }

    @Test
    fun `identify block`() {
        val board = Board(
            listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(2, 0), PlayerInfo.Two),
                MoveRequest(Coordinates(0, 1), PlayerInfo.One),
                MoveRequest(Coordinates(2, 1), PlayerInfo.Two),
            ), 3
        )

        val expected = Coordinates(0, 2)
        val actual = minimax(board, PlayerInfo.Two, PlayerInfo.One, 8, mutableMapOf())
        assertEquals(expected, actual.option, "Best move possible is the winning move")
    }

    @Test
    fun `find first move`() {
        val board = Board(emptyList(), 3)
        val actual = minimax(board, PlayerInfo.One, PlayerInfo.One, board.totalMovesAllowed(), mutableMapOf())
        println(actual)
    }
}