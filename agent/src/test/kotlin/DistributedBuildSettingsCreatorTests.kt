
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph.DistributedBuildSettings
import com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph.DistributedBuildSettingsCreator
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.agent.BuildAgentConfiguration
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DistributedBuildSettingsCreatorTests {
    private val buildAgentConfigurationMock = mockk<BuildAgentConfiguration>(relaxed = true)

    companion object {
        private fun Map<String, String>.prependInternalPrefixToKeys() =
            mapKeys {
                "build-graph.internal-settings.${it.key}"
            }

        @JvmStatic
        fun generateHappyPathTestCases() =
            listOf(
                TestCase(
                    runnerParameters =
                        mapOf(
                            "type" to "setup",
                            "exported-graph-path" to "foo",
                            "composite-build-id" to "1",
                        ).prependInternalPrefixToKeys(),
                    agentParameters =
                        mapOf(
                            "unreal-engine.build-graph.agent.shared-dir" to "/tmp/foo",
                        ),
                    expectedSettings =
                        DistributedBuildSettings.SetupBuildSettings(
                            "foo",
                            "/tmp/foo",
                            "1",
                        ),
                ),
                TestCase(
                    runnerParameters =
                        mapOf(
                            "type" to "regular",
                            "composite-build-id" to "1",
                            "node-execution-notification.enabled" to "true",
                        ).prependInternalPrefixToKeys(),
                    agentParameters =
                        mapOf(
                            "unreal-engine.build-graph.agent.shared-dir" to "/tmp/foo",
                        ),
                    expectedSettings =
                        DistributedBuildSettings.RegularBuildSettings(
                            "/tmp/foo",
                            "1",
                        ),
                ),
            )
    }

    data class TestCase(
        val runnerParameters: Map<String, String>,
        val agentParameters: Map<String, String>,
        val expectedSettings: DistributedBuildSettings,
    )

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should create settings`(case: TestCase) {
        // arrange
        every { buildAgentConfigurationMock.configurationParameters } returns case.agentParameters
        val factory = DistributedBuildSettingsCreator(buildAgentConfigurationMock)

        // act
        val result = either { factory.from(case.runnerParameters) }.getOrNull()

        // assert
        assertNotNull(result)
        assertEquals(case.expectedSettings, result)
    }
}
