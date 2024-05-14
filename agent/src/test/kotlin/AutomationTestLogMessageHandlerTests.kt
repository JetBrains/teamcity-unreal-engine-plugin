import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.AutomationTestLogMessageHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildProgressLogger
import kotlin.test.Test
import kotlin.test.assertEquals

class AutomationTestLogMessageHandlerTests {
    private val buildLoggerMock = mockk<BuildProgressLogger>(relaxed = true)
    private val buildContext = UnrealBuildContextStub(
        build = mockk<AgentRunningBuild> {
            every { buildLogger } returns buildLoggerMock
        },
    )
    private val handler = with(buildContext) { AutomationTestLogMessageHandler() }

    @Test
    fun `should write test service messages in right order`() {
        // arrange
        val lines = listOf(
            "LogAutomationController: Display: Test Started. Name={Scalar} Path={Input Test.Modifiers.Scalar}",
            "LogAutomationController: Display: Test Completed. Result={Success} Name={Scalar} Path={Input Test.Modifiers.Scalar}",
        )

        // act
        val handledCount = lines.count { handler.tryHandleMessage(it) }

        // assert
        assertEquals(2, handledCount)
        verifyOrder {
            buildLoggerMock.let {
                it.message("##teamcity[testStarted name='Input_Test.Modifiers.Scalar' captureStandardOutput='true']")
                it.message("LogAutomationController: Display: Test Started. Name={Scalar} Path={Input Test.Modifiers.Scalar}")
                it.message(
                    "LogAutomationController: Display: Test Completed. Result={Success} Name={Scalar} Path={Input Test.Modifiers.Scalar}",
                )
                it.message("##teamcity[testFinished name='Input_Test.Modifiers.Scalar' duration='-1']")
            }
        }
    }

    @Test
    fun `should not handle other messages`() {
        // arrange
        val lines = listOf(
            "LogAudio: Display: Audio Device unregistered from world 'None'",
            "LogAudio: Display: Audio Device (ID: 1) registered with world 'Untitled'.",
        )

        // act
        val handledCount = lines.count { handler.tryHandleMessage(it) }

        // assert
        assertEquals(0, handledCount)
        verify(exactly = 0) { buildLoggerMock.message(any()) }
    }
}
