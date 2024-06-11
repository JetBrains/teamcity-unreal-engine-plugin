import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphOption
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphOptionsParameter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BuildGraphOptionsParameterTests {
    companion object {
        private val acceptableSeparators = listOf("\r", "\n", "\r\n")

        @JvmStatic
        fun generateHappyPathTestCases(): List<TestCase> =
            listOf(
                TestCase(emptyMap()),
                TestCase(
                    mapOf(BuildGraphOptionsParameter.name to "Foo=Bar"),
                    listOf(BuildGraphOption("Foo", "Bar")),
                ),
                TestCase(
                    mapOf(BuildGraphOptionsParameter.name to "Foo="),
                    listOf(BuildGraphOption("Foo", "")),
                ),
                TestCase(
                    mapOf(BuildGraphOptionsParameter.name to "\r\n\n\r"),
                ),
                *acceptableSeparators
                    .map {
                        TestCase(
                            mapOf(BuildGraphOptionsParameter.name to "Foo=Bar${it}Foo2=Bar2"),
                            listOf(
                                BuildGraphOption("Foo", "Bar"),
                                BuildGraphOption("Foo2", "Bar2"),
                            ),
                        )
                    }.toTypedArray(),
            )

        @JvmStatic
        fun generateInvalidDataTestCases(): List<TestCase> =
            listOf(
                TestCase(
                    mapOf(BuildGraphOptionsParameter.name to "=Bar"),
                ),
                TestCase(
                    mapOf(BuildGraphOptionsParameter.name to "Foo=Bar="),
                ),
                TestCase(
                    mapOf(BuildGraphOptionsParameter.name to "Foo"),
                ),
            )
    }

    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedOptions: List<BuildGraphOption> = emptyList(),
    )

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should create options from the given runner parameters`(case: TestCase) {
        // act
        val result = either { BuildGraphOptionsParameter.parseOptions(case.runnerParameters) }.getOrNull()

        // assert
        assertNotNull(result)
        assertEquals(case.expectedOptions, result)
    }

    @ParameterizedTest
    @MethodSource("generateInvalidDataTestCases")
    fun `should raise an error when the given runner parameters are invalid`(case: TestCase) {
        // act
        val result = either { BuildGraphOptionsParameter.parseOptions(case.runnerParameters) }.leftOrNull()

        // assert
        assertNotNull(result)
    }
}
