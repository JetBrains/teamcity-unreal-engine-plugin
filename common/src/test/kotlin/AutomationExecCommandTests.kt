
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationExecCommandParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationFilterParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationTestsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.ExecCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.RunFilterType
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.UnrealAutomationTest
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class AutomationExecCommandTests {
    data class HappyPathTestCase(
        val runnerParameters: Map<String, String>,
        val expectedExecCommand: ExecCommand,
        val failedTestDescription: String,
    )

    private val testFilterName = "testNameFilter"

    private fun `parses automation command`(): List<HappyPathTestCase> =
        listOf(
            HappyPathTestCase(
                runnerParameters = mapOf(AutomationExecCommandParameter.name to AutomationExecCommandParameter.all.name),
                expectedExecCommand = ExecCommand.RunAll,
                failedTestDescription = "parse RunAll command",
            ),
            HappyPathTestCase(
                runnerParameters =
                    mapOf(
                        AutomationExecCommandParameter.name to AutomationExecCommandParameter.list.name,
                        AutomationTestsParameter.name to testFilterName,
                    ),
                expectedExecCommand = ExecCommand.RunTests(listOf(UnrealAutomationTest(testFilterName))),
                failedTestDescription = "parse RunTests command with non empty tests",
            ),
            HappyPathTestCase(
                runnerParameters =
                    mapOf(
                        AutomationExecCommandParameter.name to AutomationExecCommandParameter.filter.name,
                        AutomationFilterParameter.name to RunFilterType.Product.name,
                    ),
                expectedExecCommand = ExecCommand.RunFilter(RunFilterType.Product),
                failedTestDescription = "parse RunFilter command with non empty filter",
            ),
            HappyPathTestCase(
                runnerParameters =
                    mapOf(
                        AutomationExecCommandParameter.name to AutomationExecCommandParameter.all.name,
                        AutomationFilterParameter.name to RunFilterType.Product.name,
                        AutomationTestsParameter.name to testFilterName,
                    ),
                expectedExecCommand = ExecCommand.RunAll,
                failedTestDescription = "parse RunAll command with non empty tests and filter",
            ),
        )

    @ParameterizedTest
    @MethodSource("parses automation command")
    fun `parses automation command`(case: HappyPathTestCase) {
        // act
        val result = either { AutomationExecCommandParameter.parse(case.runnerParameters) }.getOrNull()

        // assert
        result shouldNotBe null
        assertEquals(case.expectedExecCommand, result, "Failed to ${case.failedTestDescription}")
    }

    data class UnhappyPathTestCase(
        val runnerParameters: Map<String, String>,
        val expectedError: PropertyValidationError,
        val failedTestDescription: String,
    )

    private fun `raises error when parameters are invalid`(): List<UnhappyPathTestCase> =
        listOf(
            UnhappyPathTestCase(
                runnerParameters = mapOf(AutomationExecCommandParameter.name to AutomationExecCommandParameter.list.name),
                expectedError = PropertyValidationError(AutomationTestsParameter.name, "Empty list of test names."),
                failedTestDescription = "return expected error when command is RunTests but the list of tests is empty",
            ),
            UnhappyPathTestCase(
                runnerParameters = mapOf(AutomationExecCommandParameter.name to AutomationExecCommandParameter.filter.name),
                expectedError = PropertyValidationError(AutomationFilterParameter.name, "Empty test filter."),
                failedTestDescription = "return expected error when command is RunFilter but the filter is empty",
            ),
        )

    @ParameterizedTest
    @MethodSource("raises error when parameters are invalid")
    fun `raises error when parameters are invalid`(case: UnhappyPathTestCase) {
        // act
        val result = either { AutomationExecCommandParameter.parse(case.runnerParameters) }.leftOrNull()

        // assert
        result shouldNotBe null
        assertEquals(case.expectedError, result, "Failed to ${case.failedTestDescription}")
    }
}
