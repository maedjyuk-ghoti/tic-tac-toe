import com.maedjyukghoti.tictactoe.logic.Board
import com.maedjyukghoti.tictactoe.logic.Coordinates
import com.maedjyukghoti.tictactoe.logic.MoveRequest
import com.maedjyukghoti.tictactoe.logic.PlayerInfo
import org.junit.Test
import kotlin.test.assertEquals

internal class CheckForWinner {
    @Test
    fun `no winner for empty board`() {
        val board = getEmptyBoard(3)
        val result = board.checkForWinner()
        assertEquals(PlayerInfo.None, result)
    }

    @Test
    fun `no winner empty squares left`() {
        val board = Board(
            moves = listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(0, 2), PlayerInfo.One),
            ),
            bounds = 3
        )
        val result = board.checkForWinner()
        assertEquals(PlayerInfo.None, result)
    }

    @Test
    fun `no winner blocked by opponent`() {
        val board = Board(
            moves = listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(0, 1), PlayerInfo.Two),
                MoveRequest(Coordinates(0, 2), PlayerInfo.One),
            ),
            bounds = 3
        )
        val result = board.checkForWinner()
        assertEquals(PlayerInfo.None, result)
    }

    @Test
    fun `winner row`() {
        val board = Board(
            moves = listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(0, 1), PlayerInfo.One),
                MoveRequest(Coordinates(0, 2), PlayerInfo.One),
            ),
            bounds = 3
        )
        val result = board.checkForWinner()
        assertEquals(PlayerInfo.One, result)
    }

    @Test
    fun `winner column`() {
        val board = Board(
            moves = listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.Two),
                MoveRequest(Coordinates(1, 0), PlayerInfo.Two),
                MoveRequest(Coordinates(2, 0), PlayerInfo.Two),
            ),
            bounds = 3
        )
        val result = board.checkForWinner()
        assertEquals(PlayerInfo.Two, result)
    }

    @Test
    fun `winner diagonal 0,0 to n,n`() {
        val board = Board(
            moves = listOf(
                MoveRequest(Coordinates(0, 0), PlayerInfo.One),
                MoveRequest(Coordinates(1, 1), PlayerInfo.One),
                MoveRequest(Coordinates(2, 2), PlayerInfo.One),
            ),
            bounds = 3
        )
        val result = board.checkForWinner()
        assertEquals(PlayerInfo.One, result)
    }

    @Test
    fun `winner diagonal 0,n to n,0`() {
        val board = Board(
            moves = listOf(
                MoveRequest(Coordinates(0, 2), PlayerInfo.Two),
                MoveRequest(Coordinates(1, 1), PlayerInfo.Two),
                MoveRequest(Coordinates(2, 0), PlayerInfo.Two),
            ),
            bounds = 3
        )
        val result = board.checkForWinner()
        assertEquals(PlayerInfo.Two, result)
    }
}