
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngine
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProvider
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineSourceVersionDetector
import com.jetbrains.teamcity.plugins.unrealengine.agent.WorkflowCreationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRootPath
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineVersion
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineIdentifierParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineRootParameter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.agent.BuildAgentConfiguration
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UnrealEngineProviderTests {
    companion object {
        @JvmStatic
        fun generateHappyPathTestCases(): Collection<HappyPathTestCase> {
            return buildList {
                val automatic = HappyPathTestCase(
                    agentConfigurationParameters = mapOf(
                        "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.5.2.path" to "foo/bar",
                    ),
                    runnerProperties = mapOf(
                        EngineDetectionModeParameter.name to EngineDetectionModeParameter.automatic.name,
                        UnrealEngineIdentifierParameter.name to "5.2",
                    ),
                    expectedRootLocation = "foo/bar",
                )
                add(automatic)

                val manual = HappyPathTestCase(
                    agentConfigurationParameters = mapOf(),
                    runnerProperties = mapOf(
                        EngineDetectionModeParameter.name to EngineDetectionModeParameter.manual.name,
                        UnrealEngineRootParameter.name to "/foo/bar/custom-location",
                    ),
                    expectedRootLocation = "/foo/bar/custom-location",
                )
                add(manual)

                val latestAutomaticRegularVersions = HappyPathTestCase(
                    agentConfigurationParameters = mapOf(
                        "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.5.1.1.path" to "foo/bar/4",
                        "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.5.1.2.path" to "foo/bar/3",
                        "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.5.3.1.path" to "foo/bar/2",
                        "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.5.3.2.path" to "foo/bar/1",
                        "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.4.2.7.path" to "foo/bar/5",
                    ),
                    runnerProperties = mapOf(
                        EngineDetectionModeParameter.name to EngineDetectionModeParameter.automatic.name,
                        UnrealEngineIdentifierParameter.name to "5",
                    ),
                    expectedRootLocation = "foo/bar/1",
                )
                add(latestAutomaticRegularVersions)

                val latestAutomaticCustomVersions = HappyPathTestCase(
                    agentConfigurationParameters = mapOf(
                        "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.ver.path" to "foo/bar/4",
                        "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.vers.path" to "foo/bar/3",
                        "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.versi.path" to "foo/bar/2",
                        "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.version.path" to "foo/bar/1",
                    ),
                    runnerProperties = mapOf(
                        EngineDetectionModeParameter.name to EngineDetectionModeParameter.automatic.name,
                        UnrealEngineIdentifierParameter.name to "ve",
                    ),
                    expectedRootLocation = "foo/bar/1",
                )
                add(latestAutomaticCustomVersions)
            }
        }
    }

    data class HappyPathTestCase(
        val agentConfigurationParameters: Map<String, String>,
        val runnerProperties: Map<String, String>,
        val expectedRootLocation: String,
    )

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should return engine path and version`(testCase: HappyPathTestCase) = runTest {
        // arrange
        val expectedVersion = UnrealEngineVersion(5, 3, 2)
        val agentConfigurationMock = mockk<BuildAgentConfiguration> {
            every { configurationParameters } returns testCase.agentConfigurationParameters
        }
        val engineVersionDetectorMock = createVersionDetectorMockk(expectedVersion)
        val provider = UnrealEngineProvider(agentConfigurationMock, engineVersionDetectorMock)

        // act
        val root = provider.act(testCase.runnerProperties, UnrealBuildContextStub())

        // assert
        assertNotNull(root)
        assertEquals(testCase.expectedRootLocation, root.path.value)
        assertEquals(expectedVersion, root.version)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "./custom-relative-location",
            "./",
            "custom-relative-location",
        ],
    )
    fun `should look up the engine relative to the current working directory when in manual mode`(relativeRootPath: String) = runTest {
        // arrange
        val agentConfigurationMock = mockk<BuildAgentConfiguration> {
            every { configurationParameters } returns mapOf()
        }
        val engineVersionDetectorMock = createVersionDetectorMockk(UnrealEngineVersion(5, 3, 2))

        val rootProvider = UnrealEngineProvider(agentConfigurationMock, engineVersionDetectorMock)
        val runnerProperties = mapOf(
            EngineDetectionModeParameter.name to EngineDetectionModeParameter.manual.name,
            UnrealEngineRootParameter.name to relativeRootPath,
        )

        val context = UnrealBuildContextStub(isAbsoluteStub = { false })
        val expectedRootLocation = UnrealEngineRootPath(context.concatPaths(context.workingDirectory, relativeRootPath))

        // act
        val root = rootProvider.act(runnerProperties, context)

        // assert
        assertNotNull(root)
        assertEquals(root.path, expectedRootLocation)
    }

    private fun createVersionDetectorMockk(detectedVersion: UnrealEngineVersion) = mockk<UnrealEngineSourceVersionDetector> {
        coEvery {
            with(any<UnrealBuildContext>()) {
                with(any<Raise<WorkflowCreationError>>()) {
                    detect(any())
                }
            }
        } returns detectedVersion
    }

    private suspend fun UnrealEngineProvider.act(runnerProperties: Map<String, String>, context: UnrealBuildContext): UnrealEngine? {
        return either {
            with(context) {
                findEngine(runnerProperties)
            }
        }.getOrNull()
    }
}
