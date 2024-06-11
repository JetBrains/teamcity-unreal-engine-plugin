package com.jetbrains.teamcity.plugins.unrealengine.agent.reporting

import jetbrains.buildServer.messages.serviceMessages.TestFailed
import jetbrains.buildServer.messages.serviceMessages.TestFinished
import jetbrains.buildServer.messages.serviceMessages.TestIgnored
import jetbrains.buildServer.messages.serviceMessages.TestStarted

// duration calculated by TeamCity based on TestStarted and TestFinished service messages timestamp
const val DURATION_CALCULATED = -1

fun TestStartedInfo.asServiceMessages() =
    sequence {
        val formattedTestName = TeamCityTestNameFormatter.format(name, fullName)
        yield(TestStarted(formattedTestName, true, null))
    }

fun TestCompletedInfo.asServiceMessages() =
    sequence {
        val formattedTestName = TeamCityTestNameFormatter.format(name, fullName)
        when (result) {
            TestResult.Success ->
                yield(
                    TestFinished(
                        formattedTestName,
                        DURATION_CALCULATED,
                    ),
                )

            TestResult.Fail -> {
                yield(TestFailed(formattedTestName, null as String?))
                yield(TestFinished(formattedTestName, DURATION_CALCULATED))
            }

            TestResult.Skipped -> yield(TestIgnored(formattedTestName, ""))
            TestResult.Unknown -> {}
        }
    }
