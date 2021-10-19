import org.junit.Test
import players.getWinningCoordinates
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class ComputerPlayerTest {
    @Test
    fun `no winning move, empty board`() {
        val board = Board(emptyList(), 3)
        val result = getWinningCoordinates(board, Player.One)
        assertNotNull(result.component2())
    }

    @Test
    fun `no winning move`() {
        val moves = listOf(
            MoveRequest(Coordinates(0,0), Player.One, 0),
            MoveRequest(Coordinates(1,2), Player.One, 2),
            MoveRequest(Coordinates(2, 1), Player.One, 4)
        )
        val board = Board(moves, 3)
        val result = getWinningCoordinates(board, Player.One)
        assertNotNull(result.component2())
    }

    @Test
    fun `winning move row`() {
        val moves = listOf(
            MoveRequest(Coordinates(0,0), Player.One, 0),
            MoveRequest(Coordinates(1,0), Player.One, 2)
        )
        val board = Board(moves, 3)
        val result = getWinningCoordinates(board, Player.One)
        assertNotNull(result.component1())
        assertEquals(result.component1(), Coordinates(2, 0))
    }

    @Test
    fun `winning move column`() {
        val moves = listOf(
            MoveRequest(Coordinates(0, 0), Player.One, 0),
            MoveRequest(Coordinates(0, 2), Player.One, 2)
        )
        val board = Board(moves, 3)
        val result = getWinningCoordinates(board, Player.One)
        assertNotNull(result.component1())
        assertEquals(result.component1(), Coordinates(0, 1))
    }

    @Test
    fun `winning move diagonal`() {
        val moves = listOf(
            MoveRequest(Coordinates(0,0), Player.One, 0),
            MoveRequest(Coordinates(1,1), Player.One, 2)
        )
        val board = Board(moves, 3)
        val result = getWinningCoordinates(board, Player.One)
        assertNotNull(result.component1())
        assertEquals(result.component1(), Coordinates(2, 2))
    }

    @Test
    fun `winning move anti-diagonal`() {
        val moves = listOf(
            MoveRequest(Coordinates(0,2), Player.One, 0),
            MoveRequest(Coordinates(2,0), Player.One, 2)
        )
        val board = Board(moves, 3)
        val result = getWinningCoordinates(board, Player.One)
        assertNotNull(result.component1())
        assertEquals(result.component1(), Coordinates(1, 1))
    }
}