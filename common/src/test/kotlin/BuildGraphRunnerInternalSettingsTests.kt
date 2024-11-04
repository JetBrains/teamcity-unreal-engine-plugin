
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphRunnerInternalSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.toMap
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BuildGraphRunnerInternalSettingsTests {
    data class TestCase(
        val settings: BuildGraphRunnerInternalSettings,
        val expectedMap: Map<String, String>,
    )

    private fun `converts itself to a map`(): List<TestCase> =
        listOf(
            TestCase(
                settings = BuildGraphRunnerInternalSettings.RegularBuildSettings("1"),
                expectedMap =
                    mapOf(
                        "build-graph.internal-settings.type" to "regular",
                        "build-graph.internal-settings.composite-build-id" to "1",
                    ),
            ),
            TestCase(
                settings = BuildGraphRunnerInternalSettings.SetupBuildSettings("/foo/bar", "1"),
                expectedMap =
                    mapOf(
                        "build-graph.internal-settings.type" to "setup",
                        "build-graph.internal-settings.exported-graph-path" to "/foo/bar",
                        "build-graph.internal-settings.composite-build-id" to "1",
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("converts itself to a map")
    fun `converts itself to a map`(case: TestCase) {
        // arrange, act
        val result = case.settings.toMap()

        // assert
        result shouldBe case.expectedMap
    }
}
