
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationExecCommandParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationFilterParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationTestsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.ExecCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.RunFilterType
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.UnrealAutomationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AutomationExecCommandTests {
    companion object {
        data class HappyTestCase(
            val description: String,
            val runnerParameters: Map<String, String>,
            val expectedExecCommand: ExecCommand,
        )

        private const val TEST_FILTER_NAME = "testNameFilter"

        @JvmStatic
        fun generateHappyPathTestCases(): List<HappyTestCase> =
            listOf(
                HappyTestCase(
                    "parse RunAll command",
                    mapOf(AutomationExecCommandParameter.name to AutomationExecCommandParameter.all.name),
                    ExecCommand.RunAll,
                ),
                HappyTestCase(
                    "parse RunTests command with non empty tests",
                    mapOf(
                        AutomationExecCommandParameter.name to AutomationExecCommandParameter.list.name,
                        AutomationTestsParameter.name to TEST_FILTER_NAME,
                    ),
                    ExecCommand.RunTests(listOf(UnrealAutomationTest(TEST_FILTER_NAME))),
                ),
                HappyTestCase(
                    "parse RunFilter command with non empty filter",
                    mapOf(
                        AutomationExecCommandParameter.name to AutomationExecCommandParameter.filter.name,
                        AutomationFilterParameter.name to RunFilterType.Product.name,
                    ),
                    ExecCommand.RunFilter(RunFilterType.Product),
                ),
                HappyTestCase(
                    "parse RunAll command with non empty tests and filter",
                    mapOf(
                        AutomationExecCommandParameter.name to AutomationExecCommandParameter.all.name,
                        AutomationFilterParameter.name to RunFilterType.Product.name,
                        AutomationTestsParameter.name to TEST_FILTER_NAME,
                    ),
                    ExecCommand.RunAll,
                ),
            )

        data class UnhappyTestCase(
            val description: String,
            val runnerParameters: Map<String, String>,
            val expectedError: PropertyValidationError,
        )

        @JvmStatic
        fun generateInvalidDataTestCases(): List<UnhappyTestCase> =
            listOf(
                UnhappyTestCase(
                    "return expected error when command is RunTests but the list of tests is empty",
                    mapOf(AutomationExecCommandParameter.name to AutomationExecCommandParameter.list.name),
                    PropertyValidationError(AutomationTestsParameter.name, "Empty list of test names."),
                ),
                UnhappyTestCase(
                    "return expected error when command is RunFilter but the filter is empty",
                    mapOf(AutomationExecCommandParameter.name to AutomationExecCommandParameter.filter.name),
                    PropertyValidationError(AutomationFilterParameter.name, "Empty test filter."),
                ),
            )
    }

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should parse automation command`(case: HappyTestCase) {
        // act
        val result = either { AutomationExecCommandParameter.parse(case.runnerParameters) }.getOrNull()

        // assert
        assertNotNull(result)
        assertEquals(case.expectedExecCommand, result, "Failed to ${case.description}")
    }

    @ParameterizedTest
    @MethodSource("generateInvalidDataTestCases")
    fun `should parse automation command`(case: UnhappyTestCase) {
        // act
        val result = either { AutomationExecCommandParameter.parse(case.runnerParameters) }.leftOrNull()

        // assert
        assertNotNull(result)
        assertEquals(case.expectedError, result, "Failed to ${case.description}")
    }
}
