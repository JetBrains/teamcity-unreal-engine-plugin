
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
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test

class BuildGraphExecCommandTests {
    companion object {
        val happyPathCaseParams =
            mapOf(
                BuildGraphScriptPathParameter.name to "BuildGraph.xml",
                BuildGraphTargetNodeParameter.name to "Build Linux",
                BuildGraphOptionsParameter.name to "Foo=Bar",
            )
    }

    private val workingDir = "FOO"

    private val commandExecutionContext =
        CommandExecutionContextStub(
            workingDirectory = workingDir,
        )

    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedCommand: BuildGraphCommand,
        val shouldContainItems: List<String>,
        val shouldNotContainItems: List<String>,
    )

    private fun `happy path test cases`(): List<TestCase> =
        listOf(
            TestCase(
                runnerParameters =
                    happyPathCaseParams +
                        mapOf(
                            AdditionalArgumentsParameter.name to "-P4 -Submit",
                        ),
                expectedCommand =
                    BuildGraphCommand(
                        BuildGraphScriptPath("BuildGraph.xml"),
                        BuildGraphTargetNode("Build Linux"),
                        listOf(BuildGraphOption("Foo", "Bar")),
                        BuildGraphMode.SingleMachine,
                        listOf("-P4", "-Submit"),
                    ),
                shouldContainItems = listOf("-script=FOO/BuildGraph.xml", "-target=Build Linux", "-set:Foo=Bar", "-P4", "-Submit"),
                shouldNotContainItems = emptyList(),
            ),
            TestCase(
                runnerParameters =
                    happyPathCaseParams +
                        mapOf(
                            BuildGraphOptionsParameter.name to "Foo=Bar\rFoo2=Bar2",
                        ),
                expectedCommand =
                    BuildGraphCommand(
                        BuildGraphScriptPath("BuildGraph.xml"),
                        BuildGraphTargetNode("Build Linux"),
                        listOf(
                            BuildGraphOption("Foo", "Bar"),
                            BuildGraphOption("Foo2", "Bar2"),
                        ),
                        BuildGraphMode.SingleMachine,
                    ),
                shouldContainItems = listOf("-script=FOO/BuildGraph.xml", "-target=Build Linux", "-set:Foo=Bar", "-set:Foo2=Bar2"),
                shouldNotContainItems = emptyList(),
            ),
        )

    @ParameterizedTest
    @MethodSource("happy path test cases")
    fun `parses command from the given runner parameters`(case: TestCase) {
        // act
        val command = either { BuildGraphCommand.from(case.runnerParameters) }.getOrNull()

        // assert
        command shouldBe case.expectedCommand
    }

    @ParameterizedTest
    @MethodSource("happy path test cases")
    fun `generates a correct list of arguments`(case: TestCase) {
        // act
        val arguments =
            either {
                with(commandExecutionContext) {
                    case.expectedCommand.toArguments()
                }
            }.getOrNull()

        // assert
        arguments shouldNotBe null
        arguments!! shouldContainAll case.shouldContainItems
        if (case.shouldNotContainItems.isNotEmpty()) {
            arguments shouldNotContainAnyOf case.shouldNotContainItems
        }
    }

    @Test
    fun `raises an error when a script file doesn't exist`() {
        // arrange
        val context =
            CommandExecutionContextStub(
                fileExistsStub = { false },
            )

        val command =
            BuildGraphCommand(
                BuildGraphScriptPath(""),
                BuildGraphTargetNode(""),
                emptyList(),
                BuildGraphMode.SingleMachine,
                emptyList(),
            )

        // act
        val error =
            either {
                with(context) { command.toArguments() }
            }.leftOrNull()

        // assert
        error shouldNotBe null
    }
}
