import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class BoardTest {
    @Test
    fun `empty board generates all coordinates`() {
        val bounds = 3
        val board = Board(emptyList(), bounds)
        val remainingCoordinates = board.getRemainingCoordinates()
        assertEquals(bounds * bounds, remainingCoordinates.count(), "All coordinates should be available on an empty board")
        for (remainingCoordinate in remainingCoordinates) {
            assertTrue(remainingCoordinate.x in 0 until bounds, "0 <= x < bounds")
            assertTrue(remainingCoordinate.y in 0 until bounds, "0 <= y < bounds")
        }
    }

    @Test
    fun `board generates remaining coordinates`() {

    }
}