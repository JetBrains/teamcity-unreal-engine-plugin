
import com.jetbrains.teamcity.plugins.unrealengine.common.escapeHTML
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class HtmlUtilsTests {
    data class TestCase(
        val input: String,
        val expectedOutput: String,
    )

    companion object {
        @JvmStatic
        fun testData() = listOf(
            TestCase("", ""),
            TestCase(
                "This is a test string without any special characters.",
                "This is a test string without any special characters.",
            ),
            TestCase("This & that", "This &amp; that"),
            TestCase("This < that", "This &lt; that"),
            TestCase("This > that", "This &gt; that"),
            TestCase("&<>", "&amp;&lt;&gt;"),
        )
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `should correctly escape text for HTML representation`(testCase: TestCase) {
        // assert
        assertEquals(testCase.expectedOutput, testCase.input.escapeHTML())
    }
}
