
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.LogEventHandler
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.LogLevel
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealEngineProcessListener
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealLogEvent
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealLogEventParser
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import jetbrains.buildServer.agent.BuildProgressLogger
import org.junit.jupiter.api.BeforeEach
import java.time.Instant
import kotlin.test.Test

class UnrealEngineProcessListenerTests {
    private val buildLogger = mockk<BuildProgressLogger>(relaxed = true)
    private val logEventParser = mockk<UnrealLogEventParser>()
    private val logEvent =
        UnrealLogEvent(
            time = Instant.now(),
            level = LogLevel.Information,
            message = "test-message",
            channel = null,
        )

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { logEventParser.parse(any()) } returns logEvent
    }

    @Test
    fun `writes to build log without custom handlers`() {
        // arrange
        val listener = createListener()

        // act
        listener.onStandardOutput("test-message")

        // assert
        verify { buildLogger.message("test-message") }
    }

    @Test
    fun `writes to the build log if none of the handlers processed the message`() {
        // arrange
        val handler1 = mockk<LogEventHandler> { every { tryHandleEvent(any()) } returns false }
        val handler2 = mockk<LogEventHandler> { every { tryHandleEvent(any()) } returns false }
        val handler3 = mockk<LogEventHandler> { every { tryHandleEvent(any()) } returns false }
        val listener = createListener(handler1, handler2, handler3)

        // act
        listener.onStandardOutput(logEvent.message)

        // assert
        verifyOrder {
            handler1.tryHandleEvent(logEvent)
            handler2.tryHandleEvent(logEvent)
            handler3.tryHandleEvent(logEvent)
            buildLogger.message(logEvent.message)
        }
    }

    @Test
    fun `finds matching message handler`() {
        // arrange
        val handler1 = mockk<LogEventHandler> { every { tryHandleEvent(any()) } returns false }
        val handler2 = mockk<LogEventHandler> { every { tryHandleEvent(any()) } returns true }
        val handler3 = mockk<LogEventHandler> { every { tryHandleEvent(any()) } returns false }
        val listener = createListener(handler1, handler2, handler3)

        // act
        listener.onStandardOutput(logEvent.message)

        // assert
        verifyOrder {
            handler1.tryHandleEvent(logEvent)
            handler2.tryHandleEvent(logEvent)
        }
        verify(exactly = 0) {
            handler3.tryHandleEvent(logEvent)
            buildLogger.message(logEvent.message)
        }
    }

    private fun createListener(vararg handlers: LogEventHandler) =
        UnrealEngineProcessListener(
            buildLogger,
            logEventParser,
            handlers.asList(),
        )
}
