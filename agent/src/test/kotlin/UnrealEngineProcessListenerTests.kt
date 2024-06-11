import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProcessListener
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.LogMessageHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildProgressLogger
import kotlin.test.Test

class UnrealEngineProcessListenerTests {
    private val buildLoggerMock = mockk<BuildProgressLogger>(relaxed = true)
    private val buildContext =
        UnrealBuildContextStub(
            build =
                mockk<AgentRunningBuild> {
                    every { buildLogger } returns buildLoggerMock
                },
        )

    @Test
    fun `should write to build log without custom handlers`() {
        // arrange
        val listener = with(buildContext) { UnrealEngineProcessListener.create() }

        // act
        listener.onStandardOutput("test-message")

        // assert
        verify { buildLoggerMock.message("test-message") }
    }

    @Test
    fun `should write to the build log if none of the handlers processed the message`() {
        // arrange
        val message = "test-message"
        val handler1 = mockk<LogMessageHandler> { every { tryHandleMessage(any()) } returns false }
        val handler2 = mockk<LogMessageHandler> { every { tryHandleMessage(any()) } returns false }
        val handler3 = mockk<LogMessageHandler> { every { tryHandleMessage(any()) } returns false }
        val listener =
            with(buildContext) {
                UnrealEngineProcessListener.create(handler1, handler2, handler3)
            }

        // act
        listener.onStandardOutput(message)

        // assert
        verifyOrder {
            handler1.tryHandleMessage(message)
            handler2.tryHandleMessage(message)
            handler3.tryHandleMessage(message)
            buildLoggerMock.message(message)
        }
    }

    @Test
    fun `should find matching message handler`() {
        // arrange
        val message = "test-message"
        val handler1 = mockk<LogMessageHandler> { every { tryHandleMessage(any()) } returns false }
        val handler2 = mockk<LogMessageHandler> { every { tryHandleMessage(any()) } returns true }
        val handler3 = mockk<LogMessageHandler> { every { tryHandleMessage(any()) } returns false }
        val listener =
            with(buildContext) {
                UnrealEngineProcessListener.create(handler1, handler2, handler3)
            }

        // act
        listener.onStandardOutput(message)

        // assert
        verifyOrder {
            handler1.tryHandleMessage(message)
            handler2.tryHandleMessage(message)
        }
        verify(exactly = 0) {
            handler3.tryHandleMessage(message)
            buildLoggerMock.message(message)
        }
    }
}
