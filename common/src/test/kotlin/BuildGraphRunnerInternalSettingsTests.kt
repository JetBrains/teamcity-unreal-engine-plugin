import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphRunnerInternalSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.toMap
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class BuildGraphRunnerInternalSettingsTests {
    companion object {
        @JvmStatic
        fun generateHappyPathTestCases(): List<TestCase> {
            return listOf(
                TestCase(
                    BuildGraphRunnerInternalSettings.RegularBuildSettings("1"),
                    mapOf(
                        "build-graph.internal-settings.type" to "regular",
                        "build-graph.internal-settings.compositeBuildId" to "1",
                    ),
                ),
                TestCase(
                    BuildGraphRunnerInternalSettings.SetupBuildSettings("/foo/bar", "1"),
                    mapOf(
                        "build-graph.internal-settings.type" to "setup",
                        "build-graph.internal-settings.exportedGraphPath" to "/foo/bar",
                        "build-graph.internal-settings.compositeBuildId" to "1",
                    ),
                ),
            )
        }
    }

    data class TestCase(
        val settings: BuildGraphRunnerInternalSettings,
        val expectedMap: Map<String, String>,
    )

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should correctly convert itself to a map`(case: TestCase) {
        // arrange, act
        val result = case.settings.toMap()

        // assert
        assertEquals(case.expectedMap, result)
    }
}
