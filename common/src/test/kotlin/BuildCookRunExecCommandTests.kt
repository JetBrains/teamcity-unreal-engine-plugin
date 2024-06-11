
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BuildCookRunExecCommandTests {
    private val contextStub = CommandExecutionContextStub(workingDirectory = WORKING_DIR)

    companion object {
        private const val WORKING_DIR = "FOO"

        @JvmStatic
        fun generateHappyPathTestCases(): List<HappyPathTestCase> =
            listOf(
                HappyPathTestCase(
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "some-path",
                        BuildConfigurationParameter.name to "StandaloneGame",
                        UnrealTargetConfigurationsParameter.Standalone.name to "Shipping+Development",
                        UnrealTargetPlatformsParameter.Standalone.name to "Mac+IOS",
                    ),
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
                    listOf(
                        "BuildCookRun",
                        "-project=${WORKING_DIR}/some-path",
                        "-build",
                        "-configuration=Shipping+Development",
                        "-targetplatform=Mac+IOS",
                        "-skipcook",
                        "-skipstage",
                    ),
                ),
                HappyPathTestCase(
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "some-path",
                        BuildConfigurationParameter.name to "StandaloneGame",
                        UnrealTargetConfigurationsParameter.Standalone.name to "Shipping",
                        UnrealTargetPlatformsParameter.Standalone.name to "IOS",
                        AdditionalArgumentsParameter.name to "-foo -bar",
                    ),
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
                    listOf(
                        "BuildCookRun",
                        "-project=${WORKING_DIR}/some-path",
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
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "some-path",
                        BuildConfigurationParameter.name to "ClientAndServer",
                        UnrealTargetConfigurationsParameter.Client.name to "Shipping",
                        UnrealTargetPlatformsParameter.Client.name to "IOS",
                        UnrealTargetConfigurationsParameter.Server.name to "Shipping",
                        UnrealTargetPlatformsParameter.Server.name to "Linux+LinuxArm64",
                    ),
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
                    listOf(
                        "BuildCookRun",
                        "-project=${WORKING_DIR}/some-path",
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
                    mapOf(
                        BuildCookRunProjectPathParameter.name to "some-path",
                        BuildConfigurationParameter.name to "Server",
                        UnrealTargetConfigurationsParameter.Server.name to "Shipping",
                        UnrealTargetPlatformsParameter.Server.name to "Linux+LinuxArm64",
                    ),
                    BuildCookRunCommand(
                        UnrealProjectPath("some-path"),
                        BuildConfiguration.Server(
                            nonEmptyListOf(UnrealTargetConfiguration.Shipping),
                            nonEmptyListOf(UnrealTargetPlatform.Linux, UnrealTargetPlatform.LinuxArm64),
                        ),
                    ),
                    listOf(
                        "BuildCookRun",
                        "-project=${WORKING_DIR}/some-path",
                        "-build",
                        "-serverconfig=Shipping",
                        "-servertargetplatform=Linux+LinuxArm64",
                        "-skipcook",
                        "-skipstage",
                    ),
                ),
            )

        @JvmStatic
        fun generateInvalidDataTestCases(): List<UnhappyPathTestCase> =
            listOf(
                UnhappyPathTestCase(mapOf()),
                UnhappyPathTestCase(
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
    }

    data class UnhappyPathTestCase(
        val runnerParameters: Map<String, String>,
    )

    data class HappyPathTestCase(
        val runnerParameters: Map<String, String>,
        val parsedCommand: BuildCookRunCommand,
        val expectedArguments: List<String>,
    )

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should correctly parse command from runner parameters`(testCase: HappyPathTestCase) {
        val result = act(testCase.runnerParameters)

        val command = result.getOrNull()
        assertNotNull(command)
        assertEquals(testCase.parsedCommand, command)
    }

    @ParameterizedTest
    @MethodSource("generateInvalidDataTestCases")
    fun `should raise error in case of invalid runner parameters`(testCase: UnhappyPathTestCase) {
        val result = act(testCase.runnerParameters)

        assertTrue(result.isLeft())
        val error = result.leftOrNull()
        assertNotNull(error)
    }

    @Test
    fun `should raise an error when converting to arguments with a non-existent specified project file`() {
        // arrange
        val context =
            CommandExecutionContextStub(
                fileExistsStub = { false },
            )

        val command =
            BuildCookRunCommand(
                UnrealProjectPath(""),
                BuildConfiguration.Standalone(
                    nonEmptyListOf(UnrealTargetConfiguration.Development),
                    nonEmptyListOf(UnrealTargetPlatform.Mac),
                ),
            )

        // act
        val error = with(context) { command.toArguments() }.leftOrNull()

        // assert
        assertNotNull(error)
    }

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should produce correct list of arguments`(case: HappyPathTestCase) {
        // act
        val arguments =
            with(contextStub) {
                case.parsedCommand.toArguments()
            }.getOrNull()

        // assert
        assertNotNull(arguments)
        assertTrue(arguments.containsAll(case.expectedArguments))
    }

    private fun act(parameters: Map<String, String>) =
        either {
            BuildCookRunCommand.from(parameters)
        }
}
