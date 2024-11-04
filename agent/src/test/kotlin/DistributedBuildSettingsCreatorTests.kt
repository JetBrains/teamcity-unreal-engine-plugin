
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph.DistributedBuildSettings
import com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph.DistributedBuildSettingsCreator
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.agent.BuildAgentConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class DistributedBuildSettingsCreatorTests {
    private val buildAgentConfigurationMock = mockk<BuildAgentConfiguration>(relaxed = true)

    private fun Map<String, String>.prependInternalPrefixToKeys() =
        mapKeys {
            "build-graph.internal-settings.${it.key}"
        }

    @BeforeEach
    fun init() {
        clearAllMocks()
    }

    private fun `creates settings`() =
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

    data class TestCase(
        val runnerParameters: Map<String, String>,
        val agentParameters: Map<String, String>,
        val expectedSettings: DistributedBuildSettings,
    )

    @ParameterizedTest
    @MethodSource("creates settings")
    fun `creates settings`(case: TestCase) {
        // arrange
        every { buildAgentConfigurationMock.configurationParameters } returns case.agentParameters
        val factory = DistributedBuildSettingsCreator(buildAgentConfigurationMock)

        // act
        val result = either { factory.from(case.runnerParameters) }.getOrNull()

        // assert
        result shouldNotBe null
        result shouldBe case.expectedSettings
    }
}
