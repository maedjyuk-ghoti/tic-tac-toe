import com.maedjyukghoti.tictactoe.logic.*
import org.junit.Test
import kotlin.test.assertNotNull

internal class ValidateTest {
    @Test
    fun `move on empty board is valid`() {
        val board = getEmptyBoard(3)
        val request = MoveRequest(Coordinates(0, 0), PlayerInfo.One)
        val result = validate(request, board)
        assertNotNull(result.component1())
    }

    @Test
    fun `move out of bounds is invalid`() {
        val board = getEmptyBoard(3)
        val request = MoveRequest(Coordinates(4, 4), PlayerInfo.One)
        val result = validate(request, board)
        assertNotNull(result.component2())
    }

    @Test
    fun `move on an open square is valid`() {
        val board = Board(
            listOf(
                MoveRequest(Coordinates(0, 1), PlayerInfo.Two),
                MoveRequest(Coordinates(0, 2), PlayerInfo.One),
                MoveRequest(Coordinates(1, 2), PlayerInfo.Two)
            ), 3
        )
        val request = MoveRequest(Coordinates(1, 1), PlayerInfo.One)
        val result = validate(request, board)
        assertNotNull(result.component1())
    }

    @Test
    fun `move on an occupied square is invalid`() {
        val board = Board(listOf(MoveRequest(Coordinates(1, 1), PlayerInfo.Two)), 3)
        val request = MoveRequest(Coordinates(1, 1), PlayerInfo.One)
        val result = validate(request, board)
        assertNotNull(result.component2())
    }
}