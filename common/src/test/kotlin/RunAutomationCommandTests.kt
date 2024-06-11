
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.ExecCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.RunAutomationCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.RunFilterType
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.UnrealAutomationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertContains
import kotlin.test.assertNotNull

class RunAutomationCommandTests {
    private val commandExecutionContext = CommandExecutionContextStub()

    companion object {
        private val testProject = UnrealProjectPath("SomeName.uproject")

        data class TestCase(
            val description: String,
            val preset: RunAutomationCommand,
            val expectedExecCmds: String,
        )

        @JvmStatic
        fun generateHappyPathTestCases(): List<TestCase> =
            listOf(
                TestCase(
                    "fill ExecCmds argument with RunAll command",
                    RunAutomationCommand(testProject, nullRHI = true, ExecCommand.RunAll),
                    "-ExecCmds=Automation RunAll;Quit;",
                ),
                TestCase(
                    "fill ExecCmds argument with RunTests command",
                    RunAutomationCommand(
                        testProject,
                        nullRHI = true,
                        ExecCommand.RunTests(listOf(UnrealAutomationTest("testNameFilter"))),
                    ),
                    "-ExecCmds=Automation RunTests testNameFilter;Quit;",
                ),
                TestCase(
                    "fill ExecCmds argument with RunFilter command",
                    RunAutomationCommand(
                        testProject,
                        nullRHI = true,
                        ExecCommand.RunFilter(RunFilterType.Product),
                    ),
                    "-ExecCmds=Automation RunFilter Product;Quit;",
                ),
            )
    }

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should correctly construct ExecCmds argument`(case: TestCase) {
        // act
        val result = with(commandExecutionContext) { case.preset.toArguments() }.getOrNull()

        // assert
        assertNotNull(result)
        assertContains(result, case.expectedExecCmds, "Failed to " + case.description)
    }
}
