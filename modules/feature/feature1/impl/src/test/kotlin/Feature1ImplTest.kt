import org.junit.Test

internal class Feature1ImplTest {

    @Test
    fun `broken test`() {
        val expected = 5

        val actual = 2 * 2

        assert(actual == expected)
    }
}