import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

fun getEmptyBoard(size: Int): Board {
    return Board(emptyMap(), size)
}

internal class ValidateTest {
    @Test
    fun `move on empty board is valid`() {
        val request = MoveRequest(
            Move(Coordinates(0,0), Player.One),
            getEmptyBoard(3)
        )
        val result = validate(request)
        assertNotNull(result.component1())
    }

    @Test
    fun `move out of bounds is invalid`() {
        val request = MoveRequest(
            Move(Coordinates(4,4), Player.One),
            getEmptyBoard(3)
        )
        val result = validate(request)
        assertNotNull(result.component2())
    }

    @Test
    fun `move on an open square is valid`() {
        val request = MoveRequest(
            Move(Coordinates(1,1), Player.One),
            Board(
                mapOf(
                    Pair(Coordinates(0,1), Player.Two),
                    Pair(Coordinates(0, 2), Player.One),
                    Pair(Coordinates(1, 2), Player.Two)
                ), 3)
        )
        val result = validate(request)
        assertNotNull(result.component1())
    }

    @Test
    fun `move on an occupied square is invalid`() {
        val request = MoveRequest(
            Move(Coordinates(1,1), Player.One),
            Board(mapOf(Pair(Coordinates(1,1), Player.Two)), 3)
        )
        val result = validate(request)
        assertNotNull(result.component2())
    }
}

internal class MakeMoveTest {
    @Test
    fun `move on board increases move count by 1`() {
        val oldBoard = getEmptyBoard(3)
        val request = MoveRequest(Move(Coordinates(1, 1), Player.Two), oldBoard)
        val newBoard = makeMove(request)

        assertTrue(oldBoard.grid.size < newBoard.grid.size, "newBoard should be larger than old board")
        assertTrue(newBoard.grid.toList().containsAll(oldBoard.grid.toList()), "oldBoard should be a subset of newBoard")
        assertTrue(newBoard.grid.contains(request.move.coordinates), "newBoard should contain the requested coordinates")
    }
}

internal class CheckForWinner {
    @Test
    fun `no winner for empty board`() {
        val board = getEmptyBoard(3)
        val result = checkForWinner(board)
        assertEquals(Player.None, result)
    }

    @Test
    fun `no winner empty squares left`() {
        val board = Board(
            grid = mapOf(
                Pair(Coordinates(0,0), Player.One),
                Pair(Coordinates(0,2), Player.One),
            ),
            bounds = 3
        )
        val result = checkForWinner(board)
        assertEquals(Player.None, result)
    }

    @Test
    fun `no winner blocked by opponent`() {
        val board = Board(
            grid = mapOf(
                Pair(Coordinates(0,0), Player.One),
                Pair(Coordinates(0,1), Player.Two),
                Pair(Coordinates(0,2), Player.One),
            ),
            bounds = 3
        )
        val result = checkForWinner(board)
        assertEquals(Player.None, result)
    }

    @Test
    fun `winner row`() {
        val board = Board(
            grid = mapOf(
                Pair(Coordinates(0,0), Player.One),
                Pair(Coordinates(0,1), Player.One),
                Pair(Coordinates(0,2), Player.One),
            ),
            bounds = 3
        )
        val result = checkForWinner(board)
        assertEquals(Player.One, result)
    }

    @Test
    fun `winner column`() {
        val board = Board(
            grid = mapOf(
                Pair(Coordinates(0,0), Player.Two),
                Pair(Coordinates(1,0), Player.Two),
                Pair(Coordinates(2,0), Player.Two),
            ),
            bounds = 3
        )
        val result = checkForWinner(board)
        assertEquals(Player.Two, result)
    }

    @Test
    fun `winner diagonal 0,0 to n,n`() {
        val board = Board(
            grid = mapOf(
                Pair(Coordinates(0,0), Player.One),
                Pair(Coordinates(1,1), Player.One),
                Pair(Coordinates(2,2), Player.One),
            ),
            bounds = 3
        )
        val result = checkForWinner(board)
        assertEquals(Player.One, result)
    }

    @Test
    fun `winner diagonal 0,n to n,0`() {
        val board = Board(
            grid = mapOf(
                Pair(Coordinates(0,2), Player.Two),
                Pair(Coordinates(1,1), Player.Two),
                Pair(Coordinates(2,0), Player.Two),
            ),
            bounds = 3
        )
        val result = checkForWinner(board)
        assertEquals(Player.Two, result)
    }
}