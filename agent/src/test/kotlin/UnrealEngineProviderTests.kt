
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngine
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProvider
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineSourceVersionDetector
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRootPath
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineVersion
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineIdentifierParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineRootParameter
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.agent.BuildAgentConfiguration
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class UnrealEngineProviderTests {
    private val versionDetector = mockk<UnrealEngineSourceVersionDetector>()
    private val agentConfiguration = mockk<BuildAgentConfiguration>()

    @BeforeEach
    fun init() {
        clearAllMocks()

        agentConfiguration withConfigurationParameters emptyMap()
    }

    private fun `returns engine path and version`(): Collection<HappyPathTestCase> =
        buildList {
            val automatic =
                HappyPathTestCase(
                    agentConfigurationParameters =
                        mapOf(
                            "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.5.2.path" to "foo/bar",
                        ),
                    runnerProperties =
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.automatic.name,
                            UnrealEngineIdentifierParameter.name to "5.2",
                        ),
                    expectedRootLocation = "foo/bar",
                )
            add(automatic)

            val manual =
                HappyPathTestCase(
                    agentConfigurationParameters = mapOf(),
                    runnerProperties =
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.manual.name,
                            UnrealEngineRootParameter.name to "/foo/bar/custom-location",
                        ),
                    expectedRootLocation = "/foo/bar/custom-location",
                )
            add(manual)

            val latestAutomaticRegularVersions =
                HappyPathTestCase(
                    agentConfigurationParameters =
                        mapOf(
                            "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.5.1.1.path" to "foo/bar/4",
                            "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.5.1.2.path" to "foo/bar/3",
                            "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.5.3.1.path" to "foo/bar/2",
                            "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.5.3.2.path" to "foo/bar/1",
                            "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.4.2.7.path" to "foo/bar/5",
                        ),
                    runnerProperties =
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.automatic.name,
                            UnrealEngineIdentifierParameter.name to "5",
                        ),
                    expectedRootLocation = "foo/bar/1",
                )
            add(latestAutomaticRegularVersions)

            val latestAutomaticCustomVersions =
                HappyPathTestCase(
                    agentConfigurationParameters =
                        mapOf(
                            "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.ver.path" to "foo/bar/4",
                            "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.vers.path" to "foo/bar/3",
                            "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.versi.path" to "foo/bar/2",
                            "${UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX}.version.path" to "foo/bar/1",
                        ),
                    runnerProperties =
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.automatic.name,
                            UnrealEngineIdentifierParameter.name to "ve",
                        ),
                    expectedRootLocation = "foo/bar/1",
                )
            add(latestAutomaticCustomVersions)
        }

    data class HappyPathTestCase(
        val agentConfigurationParameters: Map<String, String>,
        val runnerProperties: Map<String, String>,
        val expectedRootLocation: String,
    )

    @ParameterizedTest
    @MethodSource("returns engine path and version")
    fun `returns engine path and version`(testCase: HappyPathTestCase) =
        runTest {
            // arrange
            val expectedVersion = UnrealEngineVersion(5, 3, 2)
            versionDetector detectsVersion expectedVersion
            agentConfiguration withConfigurationParameters testCase.agentConfigurationParameters
            val provider = UnrealEngineProvider(agentConfiguration, versionDetector)

            // act
            val root = provider.act(testCase.runnerProperties, UnrealBuildContextStub())

            // assert
            root shouldBe
                UnrealEngine(
                    UnrealEngineRootPath(testCase.expectedRootLocation),
                    expectedVersion,
                )
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "./custom-relative-location",
            "./",
            "custom-relative-location",
        ],
    )
    fun `looks up the engine relative to the current working directory when in manual mode`(relativeRootPath: String) =
        runTest {
            // arrange
            val version = UnrealEngineVersion(5, 3, 2)
            versionDetector detectsVersion version
            val rootProvider = UnrealEngineProvider(agentConfiguration, versionDetector)
            val runnerProperties =
                mapOf(
                    EngineDetectionModeParameter.name to EngineDetectionModeParameter.manual.name,
                    UnrealEngineRootParameter.name to relativeRootPath,
                )

            val context = UnrealBuildContextStub(isAbsoluteStub = { false })
            val expectedRootLocation = UnrealEngineRootPath(context.resolvePath(context.workingDirectory, relativeRootPath))

            // act
            val root = rootProvider.act(runnerProperties, context)

            // assert
            root shouldBe
                UnrealEngine(
                    expectedRootLocation,
                    version,
                )
        }

    private infix fun UnrealEngineSourceVersionDetector.detectsVersion(version: UnrealEngineVersion) {
        coEvery {
            with(any<UnrealBuildContext>()) {
                with(any<Raise<GenericError>>()) {
                    detect(any())
                }
            }
        } returns version
    }

    private infix fun BuildAgentConfiguration.withConfigurationParameters(parameters: Map<String, String>) {
        every { configurationParameters } returns parameters
    }

    private suspend fun UnrealEngineProvider.act(
        runnerProperties: Map<String, String>,
        context: UnrealBuildContext,
    ): UnrealEngine? =
        either {
            with(context) {
                findEngine(runnerProperties)
            }
        }.getOrNull()
}
