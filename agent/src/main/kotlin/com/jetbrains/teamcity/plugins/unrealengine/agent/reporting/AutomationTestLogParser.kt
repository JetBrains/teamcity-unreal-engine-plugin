package com.jetbrains.teamcity.plugins.unrealengine.agent.reporting

import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers

enum class TestResult {
    Unknown,
    Success,
    Fail,
    Skipped,
}

data class TestStartedInfo(
    val name: String,
    val fullName: String,
) {
    val formattedName: String
        get() = TeamCityTestNameFormatter.format(name, fullName)
}

data class TestCompletedInfo(
    val name: String,
    val fullName: String,
    val result: TestResult,
) {
    val formattedName: String
        get() = TeamCityTestNameFormatter.format(name, fullName)
}

// Same regex approach to report tests is used in Rider (UnrealTestUnitTestRunner.cs)
// and in Unreal Engine Gauntlet (Gauntlet.AutomationLogParser.cs)
object AutomationTestLogParser {
    private val agentLogger = TeamCityLoggers.agent<AutomationTestLogEventHandler>()

    @Suppress("ktlint:standard:max-line-length")
    private val testStartedPattern =
        Regex("""(?:LogAutomationController|LogAutomationCommandLine).+Test Started. Name=\{(?<name>.+?)} Path=\{(?<path>.+?)}""")

    @Suppress("ktlint:standard:max-line-length")
    private val testCompletedPattern =
        Regex(
            """(?:LogAutomationController|LogAutomationCommandLine).+Test Completed\. Result=\{(?<result>Passed|Failed|Fail|Success|Skipped)} Name=\{(?<name>.+?)} Path=\{(?<path>.+?)}""",
        )

    fun tryParseTestStarted(text: String): TestStartedInfo? =
        testStartedPattern.find(text)?.let {
            val (name, path) = it.destructured
            return TestStartedInfo(name, path)
        }

    fun tryParseTestCompleted(text: String): TestCompletedInfo? =
        testCompletedPattern.find(text)?.let {
            val (resultText, name, path) = it.destructured
            val result = parseTestResult(resultText)

            return TestCompletedInfo(name, path, result)
        }

    private fun parseTestResult(result: String): TestResult =
        when (result) {
            "Success" -> TestResult.Success // UE5
            "Fail" -> TestResult.Fail // UE5
            "Passed" -> TestResult.Success // UE4
            "Failed" -> TestResult.Fail // UE4
            "Skipped" -> TestResult.Skipped
            else -> {
                agentLogger.warn("Unknown \"$result\" test result")

                TestResult.Unknown
            }
        }
}
