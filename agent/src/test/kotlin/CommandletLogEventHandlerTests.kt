import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.LogLevel
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealLogEvent
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.CommandletLogEventHandler
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import jetbrains.buildServer.agent.BuildProgressLogger
import org.junit.jupiter.api.BeforeEach
import java.time.Instant
import kotlin.test.Test

class CommandletLogEventHandlerTests {
    private val buildLoggerMock = mockk<BuildProgressLogger>(relaxed = true)
    private val buildContext = createTestUnrealBuildContext()
    private val handler = CommandletLogEventHandler(buildContext)

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { buildContext.build.buildLogger } returns buildLoggerMock
    }

    @Test
    fun `publishes incremental warning statistics`() {
        // arrange
        val warningEvent = createEvent(LogLevel.Warning)

        // act
        val firstHandled = handler.tryHandleEvent(warningEvent)
        val secondHandled = handler.tryHandleEvent(warningEvent)

        // assert
        firstHandled shouldBe false
        secondHandled shouldBe false
        verifyOrder {
            buildLoggerMock.message("##teamcity[buildStatisticValue key='unreal.commandlet.warnings' value='1']")
            buildLoggerMock.message("##teamcity[buildStatisticValue key='unreal.commandlet.warnings' value='2']")
        }
    }

    @Test
    fun `publishes incremental error statistics for error and critical levels`() {
        // arrange
        val errorEvent = createEvent(LogLevel.Error)
        val criticalEvent = createEvent(LogLevel.Critical)

        // act
        val firstHandled = handler.tryHandleEvent(errorEvent)
        val secondHandled = handler.tryHandleEvent(criticalEvent)

        // assert
        firstHandled shouldBe false
        secondHandled shouldBe false
        verifyOrder {
            buildLoggerMock.message("##teamcity[buildStatisticValue key='unreal.commandlet.errors' value='1']")
            buildLoggerMock.message("##teamcity[buildStatisticValue key='unreal.commandlet.errors' value='2']")
        }
    }

    @Test
    fun `ignores non-warning and non-error events`() {
        // arrange
        val lines = listOf(createEvent(LogLevel.Information), createEvent(LogLevel.Debug), createEvent(LogLevel.Trace))

        // act
        val handledCount = lines.count { handler.tryHandleEvent(it) }

        // assert
        handledCount shouldBe 0
        confirmVerified(buildLoggerMock)
    }

    private fun createEvent(level: LogLevel) =
        UnrealLogEvent(
            time = Instant.now(),
            level = level,
            message = "test-message",
            channel = null,
        )
}

