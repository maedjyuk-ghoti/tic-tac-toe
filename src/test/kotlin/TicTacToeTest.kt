import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

fun getEmptyBoard(size: Int): Board {
    return Board(emptyList(), size)
}

internal class ValidateTest {
    @Test
    fun `move on empty board is valid`() {
        val board = getEmptyBoard(3)
        val request = MoveRequest(Coordinates(0,0), PlayerInfo.One, board.getNextMoveNumber())
        val result = validate(request, board)
        assertNotNull(result.component1())
    }

    @Test
    fun `move out of bounds is invalid`() {
        val board = getEmptyBoard(3)
        val request = MoveRequest(Coordinates(4,4), PlayerInfo.One, board.getNextMoveNumber())
        val result = validate(request, board)
        assertNotNull(result.component2())
    }

    @Test
    fun `move on an open square is valid`() {
        val board = Board(
            listOf(
                MoveRequest(Coordinates(0,1), PlayerInfo.Two, 0),
                MoveRequest(Coordinates(0, 2), PlayerInfo.One, 1),
                MoveRequest(Coordinates(1, 2), PlayerInfo.Two, 2)
            ), 3)
        val request = MoveRequest(Coordinates(1,1), PlayerInfo.One, board.getNextMoveNumber())
        val result = validate(request, board)
        assertNotNull(result.component1())
    }

    @Test
    fun `move on an occupied square is invalid`() {
        val board =  Board(listOf(MoveRequest(Coordinates(1,1), PlayerInfo.Two, 0)), 3)
        val request = MoveRequest(Coordinates(1,1), PlayerInfo.One, board.getNextMoveNumber())
        val result = validate(request, board)
        assertNotNull(result.component2())
    }
}

internal class MakeMoveTest {
    @Test
    fun `move on board increases move count by 1`() {
        val oldBoard = getEmptyBoard(3)
        val request = MoveRequest(Coordinates(1, 1), PlayerInfo.Two, oldBoard.getNextMoveNumber())
        val newBoard = makeMove(request, oldBoard)

        assertTrue(oldBoard.moves.size < newBoard.moves.size, "newBoard should be larger than old board")
        assertTrue(newBoard.moves.containsAll(oldBoard.moves), "oldBoard should be a subset of newBoard")
        assertTrue(newBoard.moves.contains(request), "newBoard should contain the requested coordinates")
    }
}

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
                MoveRequest(Coordinates(0,0), PlayerInfo.One, 0),
                MoveRequest(Coordinates(0,2), PlayerInfo.One, 1),
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
                MoveRequest(Coordinates(0,0), PlayerInfo.One, 0),
                MoveRequest(Coordinates(0,1), PlayerInfo.Two, 1),
                MoveRequest(Coordinates(0,2), PlayerInfo.One, 2),
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
                MoveRequest(Coordinates(0,0), PlayerInfo.One, 0),
                MoveRequest(Coordinates(0,1), PlayerInfo.One, 1),
                MoveRequest(Coordinates(0,2), PlayerInfo.One, 2),
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
                MoveRequest(Coordinates(0,0), PlayerInfo.Two, 0),
                MoveRequest(Coordinates(1,0), PlayerInfo.Two, 1),
                MoveRequest(Coordinates(2,0), PlayerInfo.Two, 2),
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
                MoveRequest(Coordinates(0,0), PlayerInfo.One, 0),
                MoveRequest(Coordinates(1,1), PlayerInfo.One, 1),
                MoveRequest(Coordinates(2,2), PlayerInfo.One, 2),
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
                MoveRequest(Coordinates(0,2), PlayerInfo.Two, 0),
                MoveRequest(Coordinates(1,1), PlayerInfo.Two, 1),
                MoveRequest(Coordinates(2,0), PlayerInfo.Two, 2),
            ),
            bounds = 3
        )
        val result = board.checkForWinner()
        assertEquals(PlayerInfo.Two, result)
    }
}

internal class AppendWithSpace {
    @Test
    fun `no space before single digit number`() {
        val spaceCount = 1
        val number = 7
        val expected = "$number"
        val result = appendWithSpaceBefore(spaceCount, number)

        assertEquals(expected, result)
    }

    @Test
    fun `1 space before single digit number`() {
        val spaceCount = 2
        val number = 7
        val expected = " $number"
        val result = appendWithSpaceBefore(spaceCount, number)

        assertEquals(expected, result)
    }

    @Test
    fun `no space after single digit number`() {
        val spaceCount = 1
        val number = 7
        val expected = "$number"
        val result = appendWithSpaceAfter(spaceCount, number)

        assertEquals(expected, result)
    }

    @Test
    fun `1 space after single digit number`() {
        val spaceCount = 2
        val number = 7
        val expected = "$number "
        val result = appendWithSpaceAfter(spaceCount, number)

        assertEquals(expected, result)
    }
}

internal class Undo {
    @Test
    fun `undoing empty game returns error`() {
        val result = getEmptyBoard(3).undoMove()
        assertNotNull(result.component2(), "Nothing to undo")
    }

    @Test
    fun `undoing valid board returns new board`() {
        val board = Board(listOf(MoveRequest(Coordinates(0, 0), PlayerInfo.One, 0)), 3)
        val result = board.undoMove()
        assertNotNull(result.component1(), "Should be a valid undo")
        val newBoard = result.component1()!!
        assertEquals(newBoard.moves.count(), board.moves.count() - 1, "New board should have 1 fewer moves")
        assertTrue(board.moves.containsAll(newBoard.moves))
    }
}