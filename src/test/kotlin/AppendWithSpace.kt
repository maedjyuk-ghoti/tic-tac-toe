import org.junit.Test
import kotlin.test.assertEquals

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