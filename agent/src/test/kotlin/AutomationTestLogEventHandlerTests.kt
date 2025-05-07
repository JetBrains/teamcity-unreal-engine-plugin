
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.LogLevel
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealLogEvent
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.AutomationTestLogEventHandler
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

class AutomationTestLogEventHandlerTests {
    private val buildLoggerMock = mockk<BuildProgressLogger>(relaxed = true)
    private val buildContext = createTestUnrealBuildContext()
    private val handler = with(buildContext) { AutomationTestLogEventHandler() }

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { buildContext.build.buildLogger } returns buildLoggerMock
    }

    @Test
    fun `writes test service messages in the right order`() {
        // arrange
        val lines =
            listOf(
                "LogAutomationController: Display: Test Started. Name={Scalar} Path={Input Test.Modifiers.Scalar}",
                "LogAutomationController: Display: Test Completed. Result={Success} Name={Scalar} Path={Input Test.Modifiers.Scalar}",
            )

        // act
        val handledCount = lines.count { handler.tryHandleEvent(it.toLogEvent()) }

        // assert
        handledCount shouldBe 2
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
    fun `ignores other messages`() {
        // arrange
        val lines =
            listOf(
                "LogAudio: Display: Audio Device unregistered from world 'None'",
                "LogAudio: Display: Audio Device (ID: 1) registered with world 'Untitled'.",
            )

        // act
        val handledCount = lines.count { handler.tryHandleEvent(it.toLogEvent()) }

        // assert
        handledCount shouldBe 0
        confirmVerified(buildLoggerMock)
    }

    private fun String.toLogEvent() =
        UnrealLogEvent(
            time = Instant.now(),
            level = LogLevel.Information,
            message = this,
            channel = null,
        )
}
