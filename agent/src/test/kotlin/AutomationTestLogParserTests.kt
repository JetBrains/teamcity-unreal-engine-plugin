
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.AutomationTestLogParser
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.TestResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class AutomationTestLogParserTests {
    data class TestStartedMessageTestCase(
        val logMessage: String,
        val expectedTestName: String?,
    )

    private fun `parses test started message`(): Collection<TestStartedMessageTestCase> =
        listOf(
            TestStartedMessageTestCase(
                logMessage = "[976]LogAutomationController: Display: Test Started. Name={Down} Path={Input.Triggers.Down}",
                expectedTestName = "Input.Triggers.Down",
            ),
            TestStartedMessageTestCase(
                logMessage = "[997]LogAutomationController: Other log line...",
                expectedTestName = null,
            ),
            TestStartedMessageTestCase(
                logMessage = "",
                expectedTestName = null,
            ),
        )

    @ParameterizedTest
    @MethodSource("parses test started message")
    fun `parses test started message`(testCase: TestStartedMessageTestCase) {
        // act
        val testInfo = AutomationTestLogParser.tryParseTestStarted(testCase.logMessage)

        // assert
        testInfo?.fullName shouldBe testCase.expectedTestName
    }

    data class TestCompletedMessageTestCase(
        val logMessage: String,
        val expectedTestName: String?,
        val expectedTestResult: TestResult?,
    )

    private fun `parses test completed message`(): Collection<TestCompletedMessageTestCase> =
        listOf(
            TestCompletedMessageTestCase(logMessage = "", expectedTestName = null, expectedTestResult = null),
            TestCompletedMessageTestCase(
                logMessage = "[979]LogAutomationController: Display: Test Completed. Result={Success} Name={Test} Path={UE5SuccessTest}",
                expectedTestName = "UE5SuccessTest",
                expectedTestResult = TestResult.Success,
            ),
            TestCompletedMessageTestCase(
                logMessage = "[979]LogAutomationController: Display: Test Completed. Result={Passed} Name={Test} Path={UE4PassedTest}",
                expectedTestName = "UE4PassedTest",
                expectedTestResult = TestResult.Success,
            ),
            TestCompletedMessageTestCase(
                logMessage = "[979]LogAutomationController: Display: Test Completed. Result={Fail} Name={Test} Path={UE5FailTest}",
                expectedTestName = "UE5FailTest",
                expectedTestResult = TestResult.Fail,
            ),
            TestCompletedMessageTestCase(
                logMessage = "[979]LogAutomationController: Display: Test Completed. Result={Failed} Name={Test} Path={UE4FailedTest}",
                expectedTestName = "UE4FailedTest",
                expectedTestResult = TestResult.Fail,
            ),
            TestCompletedMessageTestCase(
                logMessage = "[979]LogAutomationController: Display: Test Completed. Result={Skipped} Name={Test} Path={UESkippedTest}",
                expectedTestName = "UESkippedTest",
                expectedTestResult = TestResult.Skipped,
            ),
            TestCompletedMessageTestCase(
                logMessage = "[979]LogAutomationController: Display: Test Completed. Not valid message",
                expectedTestName = null,
                expectedTestResult = null,
            ),
            TestCompletedMessageTestCase(
                logMessage = "",
                expectedTestName = null,
                expectedTestResult = null,
            ),
        )

    @ParameterizedTest
    @MethodSource("parses test completed message")
    fun `parses test completed message`(testCase: TestCompletedMessageTestCase) {
        // act
        val testInfo = AutomationTestLogParser.tryParseTestCompleted(testCase.logMessage)

        // assert
        testInfo?.fullName shouldBe testCase.expectedTestName
        testInfo?.result shouldBe testCase.expectedTestResult
    }
}
