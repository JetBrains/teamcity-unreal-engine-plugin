
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetConfiguration
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealTargetPlatform
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildConfiguration
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildConfigurationParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetConfigurationsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetPlatformsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertNotNull

class BuildCookRunExecCommandTests {
    private val context = createTestCommandExecutionContext()

    @BeforeEach
    fun init() {
        clearAllMocks()
        setupTestCommandExecutionContext(context)
    }

    data class HappyPathTestCase(
        val runnerParameters: Map<String, String>,
        val parsedCommand: BuildCookRunCommand,
        val expectedArguments: List<String>,
    )

    private fun `happy path test cases`(): List<HappyPathTestCase> =
        listOf(
            HappyPathTestCase(
                runnerParameters =
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "some-path",
                        BuildConfigurationParameter.name to "StandaloneGame",
                        UnrealTargetConfigurationsParameter.Standalone.name to "Shipping+Development",
                        UnrealTargetPlatformsParameter.Standalone.name to "Mac+IOS",
                    ),
                parsedCommand =
                    BuildCookRunCommand(
                        UnrealProjectPath("some-path"),
                        BuildConfiguration.Standalone(
                            nonEmptyListOf(
                                UnrealTargetConfiguration.Shipping,
                                UnrealTargetConfiguration.Development,
                            ),
                            nonEmptyListOf(
                                UnrealTargetPlatform.Mac,
                                UnrealTargetPlatform.IOS,
                            ),
                        ),
                    ),
                expectedArguments =
                    listOf(
                        "BuildCookRun",
                        "-project=${context.workingDirectory}/some-path",
                        "-build",
                        "-configuration=Shipping+Development",
                        "-targetplatform=Mac+IOS",
                        "-skipcook",
                        "-skipstage",
                    ),
            ),
            HappyPathTestCase(
                runnerParameters =
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "some-path",
                        BuildConfigurationParameter.name to "StandaloneGame",
                        UnrealTargetConfigurationsParameter.Standalone.name to "Shipping",
                        UnrealTargetPlatformsParameter.Standalone.name to "IOS",
                        AdditionalArgumentsParameter.name to "-foo -bar",
                    ),
                parsedCommand =
                    BuildCookRunCommand(
                        UnrealProjectPath("some-path"),
                        BuildConfiguration.Standalone(
                            nonEmptyListOf(UnrealTargetConfiguration.Shipping),
                            nonEmptyListOf(
                                UnrealTargetPlatform.IOS,
                            ),
                        ),
                        extraArguments = listOf("-foo", "-bar"),
                    ),
                expectedArguments =
                    listOf(
                        "BuildCookRun",
                        "-project=${context.workingDirectory}/some-path",
                        "-build",
                        "-configuration=Shipping",
                        "-targetplatform=IOS",
                        "-skipcook",
                        "-skipstage",
                        "-foo",
                        "-bar",
                    ),
            ),
            HappyPathTestCase(
                runnerParameters =
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "some-path",
                        BuildConfigurationParameter.name to "ClientAndServer",
                        UnrealTargetConfigurationsParameter.Client.name to "Shipping",
                        UnrealTargetPlatformsParameter.Client.name to "IOS",
                        UnrealTargetConfigurationsParameter.Server.name to "Shipping",
                        UnrealTargetPlatformsParameter.Server.name to "Linux+LinuxArm64",
                    ),
                parsedCommand =
                    BuildCookRunCommand(
                        UnrealProjectPath("some-path"),
                        BuildConfiguration.ClientAndServer(
                            nonEmptyListOf(UnrealTargetConfiguration.Shipping),
                            nonEmptyListOf(
                                UnrealTargetPlatform.IOS,
                            ),
                            nonEmptyListOf(UnrealTargetConfiguration.Shipping),
                            nonEmptyListOf(UnrealTargetPlatform.Linux, UnrealTargetPlatform.LinuxArm64),
                        ),
                    ),
                expectedArguments =
                    listOf(
                        "BuildCookRun",
                        "-project=${context.workingDirectory}/some-path",
                        "-build",
                        "-clientconfig=Shipping",
                        "-targetplatform=IOS",
                        "-serverconfig=Shipping",
                        "-servertargetplatform=Linux+LinuxArm64",
                        "-skipcook",
                        "-skipstage",
                    ),
            ),
            HappyPathTestCase(
                runnerParameters =
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "some-path",
                        BuildConfigurationParameter.name to "Server",
                        UnrealTargetConfigurationsParameter.Server.name to "Shipping",
                        UnrealTargetPlatformsParameter.Server.name to "Linux+LinuxArm64",
                    ),
                parsedCommand =
                    BuildCookRunCommand(
                        UnrealProjectPath("some-path"),
                        BuildConfiguration.Server(
                            nonEmptyListOf(UnrealTargetConfiguration.Shipping),
                            nonEmptyListOf(UnrealTargetPlatform.Linux, UnrealTargetPlatform.LinuxArm64),
                        ),
                    ),
                expectedArguments =
                    listOf(
                        "BuildCookRun",
                        "-project=${context.workingDirectory}/some-path",
                        "-build",
                        "-serverconfig=Shipping",
                        "-servertargetplatform=Linux+LinuxArm64",
                        "-skipcook",
                        "-skipstage",
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("happy path test cases")
    fun `parses command from runner parameters`(testCase: HappyPathTestCase) {
        // act
        val result = act(testCase.runnerParameters)

        // assert
        val command = result.getOrNull()
        command shouldBe testCase.parsedCommand
    }

    data class UnhappyPathTestCase(
        val runnerParameters: Map<String, String>,
    )

    private fun `raises error in case of invalid runner parameters`(): List<UnhappyPathTestCase> =
        listOf(
            UnhappyPathTestCase(runnerParameters = mapOf()),
            UnhappyPathTestCase(
                runnerParameters =
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "some-path",
                        BuildConfigurationParameter.name to BuildConfigurationParameter.standalone.name,
                        UnrealTargetConfigurationsParameter.Standalone.name to
                            UnrealTargetConfigurationsParameter.joinConfigurations(
                                listOf(UnrealTargetConfiguration.Shipping, UnrealTargetConfiguration.Development),
                            ),
                        UnrealTargetPlatformsParameter.Standalone.name to "",
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("raises error in case of invalid runner parameters")
    fun `raises error in case of invalid runner parameters`(testCase: UnhappyPathTestCase) {
        // act
        val result = act(testCase.runnerParameters)

        // assert
        val error = result.leftOrNull()
        error shouldNotBe null
    }

    @Test
    fun `raises error when converting to arguments with a non-existent specified project file`() {
        // arrange
        every { context.fileExists(any()) } returns false

        val command =
            BuildCookRunCommand(
                UnrealProjectPath(""),
                BuildConfiguration.Standalone(
                    nonEmptyListOf(UnrealTargetConfiguration.Development),
                    nonEmptyListOf(UnrealTargetPlatform.Mac),
                ),
            )

        // act
        val error = either { with(context) { command.toArguments() } }.leftOrNull()

        // assert
        error shouldNotBe null
    }

    @ParameterizedTest
    @MethodSource("happy path test cases")
    fun `produces correct list of arguments`(case: HappyPathTestCase) {
        // act
        val arguments =
            either {
                with(context) {
                    case.parsedCommand.toArguments()
                }
            }.getOrNull()

        // assert
        assertNotNull(arguments)
        arguments shouldNotBe null
        arguments shouldContainAll case.expectedArguments
    }

    private fun act(parameters: Map<String, String>) =
        either {
            BuildCookRunCommand.from(parameters)
        }
}
