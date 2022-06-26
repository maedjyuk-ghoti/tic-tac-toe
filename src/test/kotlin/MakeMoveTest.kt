import com.maedjyukghoti.tictactoe.logic.Coordinates
import com.maedjyukghoti.tictactoe.logic.MoveRequest
import com.maedjyukghoti.tictactoe.logic.PlayerInfo
import com.maedjyukghoti.tictactoe.logic.makeMove
import org.junit.Test
import kotlin.test.assertTrue

internal class MakeMoveTest {
    @Test
    fun `move on board increases move count by 1`() {
        val oldBoard = getEmptyBoard(3)
        val request = MoveRequest(Coordinates(1, 1), PlayerInfo.Two)
        val newBoard = makeMove(request, oldBoard)

        assertTrue(oldBoard.moves.size < newBoard.moves.size, "newBoard should be larger than old board")
        assertTrue(newBoard.moves.containsAll(oldBoard.moves), "oldBoard should be a subset of newBoard")
        assertTrue(newBoard.moves.contains(request), "newBoard should contain the requested coordinates")
    }
}