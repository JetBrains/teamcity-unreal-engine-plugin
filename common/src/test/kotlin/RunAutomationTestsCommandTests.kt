
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.ExecCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.RunAutomationTestsCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.RunFilterType
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.UnrealAutomationTest
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertContains

class RunAutomationTestsCommandTests {
    private val commandExecutionContext = CommandExecutionContextStub()
    private val testProject = UnrealProjectPath("SomeName.uproject")

    data class TestCase(
        val automationTestsCommand: RunAutomationTestsCommand,
        val expectedExecCmds: String,
        val failedTestDescription: String,
    )

    private fun `constructs ExecCmds argument`(): List<TestCase> =
        listOf(
            TestCase(
                automationTestsCommand = RunAutomationTestsCommand(testProject, nullRHI = true, ExecCommand.RunAll),
                expectedExecCmds = "-ExecCmds=Automation RunAll;Quit;",
                failedTestDescription = "fill ExecCmds argument with RunAll command",
            ),
            TestCase(
                automationTestsCommand =
                    RunAutomationTestsCommand(
                        testProject,
                        nullRHI = true,
                        ExecCommand.RunTests(listOf(UnrealAutomationTest("testNameFilter"))),
                    ),
                expectedExecCmds = "-ExecCmds=Automation RunTests testNameFilter;Quit;",
                failedTestDescription = "fill ExecCmds argument with RunTests command",
            ),
            TestCase(
                automationTestsCommand =
                    RunAutomationTestsCommand(
                        testProject,
                        nullRHI = true,
                        ExecCommand.RunFilter(RunFilterType.Product),
                    ),
                expectedExecCmds = "-ExecCmds=Automation RunFilter Product;Quit;",
                failedTestDescription = "fill ExecCmds argument with RunFilter command",
            ),
        )

    @ParameterizedTest
    @MethodSource("constructs ExecCmds argument")
    fun `constructs ExecCmds argument`(case: TestCase) {
        // act
        val result =
            either {
                with(commandExecutionContext) { case.automationTestsCommand.toArguments() }
            }.getOrNull()

        // assert
        result shouldNotBe null
        assertContains(result!!, case.expectedExecCmds, "Failed to " + case.failedTestDescription)
    }

    @Test
    fun `passes project as a first argument`() {
        // arrange
        val command =
            RunAutomationTestsCommand(
                testProject,
                nullRHI = true,
                execCommand = ExecCommand.RunAll,
            )
        val context = CommandExecutionContextStub(workingDirectory = "")

        // act
        val result =
            either {
                with(context) { command.toArguments() }
            }.getOrNull()

        // assert
        result
            .shouldNotBe(null)
            .shouldNotBeEmpty()
            .first()
            .shouldBe(testProject.value)
    }
}
