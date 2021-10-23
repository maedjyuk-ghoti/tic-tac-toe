package players

import Board
import Coordinates
import MoveRequest
import org.junit.Test
import kotlin.test.assertEquals

internal class AlphaBetaTest {

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

        val actual = alphabeta(board, PlayerInfo.One, PlayerInfo.Two, board.totalMovesAllowed(), Int.MIN_VALUE, Int.MAX_VALUE)
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
            ), 3
        )

        val actual = alphabeta(board, PlayerInfo.One, PlayerInfo.One, board.totalMovesAllowed(), Int.MIN_VALUE, Int.MAX_VALUE)
        val expected = Coordinates(0, 2)
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
        val actual = alphabeta(board, PlayerInfo.Two, PlayerInfo.One, board.totalMovesAllowed(), Int.MIN_VALUE, Int.MAX_VALUE)
        assertEquals(expected, actual.option, "Best move possible is the winning move")
    }
}
