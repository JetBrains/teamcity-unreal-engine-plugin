package resource.location

import com.jetbrains.teamcity.plugins.framework.resource.location.AcceptFilter
import com.jetbrains.teamcity.plugins.framework.resource.location.filteredLines
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class FilteredLinesTests {
    data class TestCase(
        val content: String,
        val acceptFilter: AcceptFilter,
        val expectedLines: List<String>,
    )

    companion object {
        @JvmStatic
        fun generateTestCases() = listOf(
            TestCase(
                """
                    foo
                    bar
                    f oo 123
                    foo bar foo
                    1234
                """.trimIndent(),
                { it.contains("foo") },
                listOf(
                    "foo",
                    "foo bar foo"
                ),
            ),
            TestCase(
                """
                    foo
                    bar
                    baz
                """.trimIndent(),
                { false },
                emptyList(),
            )
        )
    }

    @ParameterizedTest
    @MethodSource("generateTestCases")
    fun `should return only matching lines`(case: TestCase) {
        // arrange, act
        val result = StringReader(case.content).filteredLines(case.acceptFilter) { _, _ -> true }

        // assert
        assertEquals(case.expectedLines, result)
    }

    @Test
    fun `should stop reading after the corresponding callback says so`() {
        // arrange
        val content = """
            foo
            foo
            foo
            foo
            foo
        """.trimIndent()

        val numberOfLinesToRead = 3

        // act
        val result = StringReader(content).filteredLines({_ -> true}, { _, acceptedSoFar ->
            acceptedSoFar != numberOfLinesToRead
        })

        // assert
        assertEquals(numberOfLinesToRead, result.size)
    }
}
