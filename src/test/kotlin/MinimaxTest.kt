import org.junit.Test
import players.minimax
import kotlin.test.assertEquals

internal class MinimaxTest {

    @Test
    fun `identify winning move on won board`() {
        val board = Board(
            listOf(
                MoveRequest(Coordinates(0,0,), PlayerInfo.One),
                MoveRequest(Coordinates(2,2,), PlayerInfo.Two),
                MoveRequest(Coordinates(0,1,), PlayerInfo.One),
                MoveRequest(Coordinates(2,1,), PlayerInfo.Two),
                MoveRequest(Coordinates(0,2,), PlayerInfo.One),
            ), 3
        )

        val actual = minimax(board, PlayerInfo.One, PlayerInfo.Two, false, 0)
        val expected = board.moves.last()
        assertEquals(expected, actual.option, "Should return last move of a won board")
    }

    @Test
    fun `identify winning choice 1 move away`() {
        val board = Board(
            listOf(
                MoveRequest(Coordinates(0,0,), PlayerInfo.One),
                MoveRequest(Coordinates(2,2,), PlayerInfo.Two),
                MoveRequest(Coordinates(0,1,), PlayerInfo.One),
                MoveRequest(Coordinates(2,1,), PlayerInfo.Two),
            ), 3
        )

        val actual = minimax(board, PlayerInfo.One, PlayerInfo.One,true, 0)
        val expected = MoveRequest(Coordinates(0,2,), PlayerInfo.One)
        assertEquals(expected, actual.option, "Best move possible is the winning move")
    }

    @Test
    fun `identify block`() {
        val board = Board(
            listOf(
                MoveRequest(Coordinates(0,0,), PlayerInfo.One),
                MoveRequest(Coordinates(2,2,), PlayerInfo.Two),
                MoveRequest(Coordinates(0,1,), PlayerInfo.One),
                MoveRequest(Coordinates(2,1,), PlayerInfo.Two),
            ), 3
        )

        val actual = minimax(board, PlayerInfo.Two, PlayerInfo.One,false, 0)
        val expected = MoveRequest(Coordinates(0,2,), PlayerInfo.One)
        assertEquals(expected, actual.option, "Best move possible is the winning move")
    }
}