
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphMode
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphOption
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphOptionsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphScriptPath
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphScriptPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphTargetNode
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphTargetNodeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.AdditionalArgumentsParameter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BuildGraphExecCommandTests {
    private val commandExecutionContext = CommandExecutionContextStub(
        workingDirectory = WORKING_DIR,
    )

    companion object {
        const val WORKING_DIR = "FOO"
        val happyPathCaseParams = mapOf(
            BuildGraphScriptPathParameter.name to "BuildGraph.xml",
            BuildGraphTargetNodeParameter.name to "Build Linux",
            BuildGraphOptionsParameter.name to "Foo=Bar",
        )

        @JvmStatic
        fun generateHappyPathTestCases(): List<TestCase> {
            return listOf(
                TestCase(
                    happyPathCaseParams + mapOf(
                        AdditionalArgumentsParameter.name to "-P4 -Submit",
                    ),
                    BuildGraphCommand(
                        BuildGraphScriptPath("BuildGraph.xml"),
                        BuildGraphTargetNode("Build Linux"),
                        listOf(BuildGraphOption("Foo", "Bar")),
                        BuildGraphMode.SingleMachine,
                        listOf("-P4", "-Submit"),
                    ),
                    listOf("-script=FOO/BuildGraph.xml", "-target=Build Linux", "-set:Foo=Bar", "-P4", "-Submit"),
                    emptyList(),
                ),
                TestCase(
                    happyPathCaseParams + mapOf(
                        BuildGraphOptionsParameter.name to "Foo=Bar\rFoo2=Bar2",
                    ),
                    BuildGraphCommand(
                        BuildGraphScriptPath("BuildGraph.xml"),
                        BuildGraphTargetNode("Build Linux"),
                        listOf(
                            BuildGraphOption("Foo", "Bar"),
                            BuildGraphOption("Foo2", "Bar2"),
                        ),
                        BuildGraphMode.SingleMachine,
                    ),
                    listOf("-script=FOO/BuildGraph.xml", "-target=Build Linux", "-set:Foo=Bar", "-set:Foo2=Bar2"),
                    emptyList(),
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should parse command from the given runner parameters`(case: TestCase) {
        // act
        val command = either { BuildGraphCommand.from(case.runnerParameters) }.getOrNull()

        // assert
        assertNotNull(command)
        assertEquals(case.expectedCommand, command)
    }

    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedCommand: BuildGraphCommand,
        val shouldContainItems: List<String>,
        val shouldNotContainItems: List<String>,
    )

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should generate a correct list of arguments`(case: TestCase) {
        // act
        val arguments = with(commandExecutionContext) {
            case.expectedCommand.toArguments()
        }.getOrNull()

        // assert
        assertNotNull(arguments)
        assertTrue(arguments.containsAll(case.shouldContainItems))
        assertTrue(case.shouldNotContainItems.all { !arguments.contains(it) })
    }

    @Test
    fun `should raise an error when a script file doesn't exist`() {
        // arrange
        val context = CommandExecutionContextStub(
            fileExistsStub = { false },
        )

        val command =
            BuildGraphCommand(BuildGraphScriptPath(""), BuildGraphTargetNode(""), emptyList(), BuildGraphMode.SingleMachine, emptyList())

        // act
        val error = with(context) { command.toArguments() }.leftOrNull()

        // assert
        assertNotNull(error)
    }
}
