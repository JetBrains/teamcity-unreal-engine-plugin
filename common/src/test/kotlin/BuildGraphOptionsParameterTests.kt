import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphOption
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphOptionsParameter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BuildGraphOptionsParameterTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedOptions: List<BuildGraphOption> = emptyList(),
    )

    private val acceptableSeparators = listOf("\r", "\n", "\r\n")

    private fun `creates options from the given runner parameters`(): List<TestCase> =
        listOf(
            TestCase(runnerParameters = emptyMap()),
            TestCase(
                runnerParameters = mapOf(BuildGraphOptionsParameter.name to "Foo=Bar"),
                expectedOptions = listOf(BuildGraphOption("Foo", "Bar")),
            ),
            TestCase(
                runnerParameters = mapOf(BuildGraphOptionsParameter.name to "Foo="),
                expectedOptions = listOf(BuildGraphOption("Foo", "")),
            ),
            TestCase(
                runnerParameters = mapOf(BuildGraphOptionsParameter.name to "\r\n\n\r"),
            ),
            *acceptableSeparators
                .map {
                    TestCase(
                        runnerParameters = mapOf(BuildGraphOptionsParameter.name to "Foo=Bar${it}Foo2=Bar2"),
                        expectedOptions =
                            listOf(
                                BuildGraphOption("Foo", "Bar"),
                                BuildGraphOption("Foo2", "Bar2"),
                            ),
                    )
                }.toTypedArray(),
            TestCase(
                runnerParameters = mapOf(BuildGraphOptionsParameter.name to "Option=-a -b -c -bar=baz"),
                expectedOptions = listOf(BuildGraphOption("Option", "-a -b -c -bar=baz")),
            ),
            TestCase(
                runnerParameters = mapOf(BuildGraphOptionsParameter.name to "AnotherOption=value=with=equals"),
                expectedOptions = listOf(BuildGraphOption("AnotherOption", "value=with=equals")),
            ),
            TestCase(
                runnerParameters = mapOf(BuildGraphOptionsParameter.name to "MixedCase=foo${"\n"}Other=bar=baz"),
                expectedOptions =
                    listOf(
                        BuildGraphOption("MixedCase", "foo"),
                        BuildGraphOption("Other", "bar=baz"),
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("creates options from the given runner parameters")
    fun `creates options from the given runner parameters`(case: TestCase) {
        // act
        val result = either { BuildGraphOptionsParameter.parseOptions(case.runnerParameters) }.getOrNull()

        // assert
        result shouldBe case.expectedOptions
    }

    private fun `raises an error when the given runner parameters are invalid`(): List<TestCase> =
        listOf(
            TestCase(
                runnerParameters = mapOf(BuildGraphOptionsParameter.name to "=NoName"),
            ),
            TestCase(
                runnerParameters = mapOf(BuildGraphOptionsParameter.name to "JustTextNoEquals"),
            ),
        )

    @ParameterizedTest
    @MethodSource("raises an error when the given runner parameters are invalid")
    fun `raises an error when the given runner parameters are invalid`(case: TestCase) {
        // act
        val error = either { BuildGraphOptionsParameter.parseOptions(case.runnerParameters) }.leftOrNull()

        // assert
        error shouldNotBe null
    }
}
