package build.log

import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.LogLevel
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealJsonLogEvent
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealJsonLogEventParser
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealLogEvent
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealLogEventParser
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Clock
import java.time.Instant

class UnrealLogEventParserTests {
    private val jsonLogEventParser = mockk<UnrealJsonLogEventParser>()
    private val clock = mockk<Clock>()
    private val now = Instant.parse("2027-10-05T12:00:00Z")

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { clock.instant() } returns now
    }

    data class TestCase(
        val input: String,
        val structuredEvent: UnrealJsonLogEvent?,
        val expected: UnrealLogEvent,
    )

    private fun `parses structured events`() =
        listOf(
            TestCase(
                input = "valid_event_with_channel",
                structuredEvent =
                    UnrealJsonLogEvent(
                        time = now,
                        level = LogLevel.Information,
                        message = "A valid event",
                        properties =
                            mapOf(
                                "_channel" to
                                    JsonObject(
                                        mapOf(
                                            "\$text" to JsonPrimitive("TestChannel"),
                                        ),
                                    ),
                            ),
                    ),
                expected =
                    UnrealLogEvent(
                        time = now,
                        level = LogLevel.Information,
                        message = "A valid event",
                        channel = "TestChannel",
                    ),
            ),
            TestCase(
                input = "valid_event_without_channel",
                structuredEvent =
                    UnrealJsonLogEvent(
                        time = Instant.parse("2024-12-05T12:00:00Z"),
                        level = LogLevel.Error,
                        message = "An error occurred",
                        properties = null,
                    ),
                expected =
                    UnrealLogEvent(
                        time = Instant.parse("2024-12-05T12:00:00Z"),
                        level = LogLevel.Error,
                        message = "An error occurred",
                        channel = null,
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("parses structured events")
    fun `parses structured events`(case: TestCase) {
        // arrange
        every { jsonLogEventParser.parse(case.input) } returns case.structuredEvent

        // act
        val result = createInstance().parse(case.input)

        // assert
        result shouldBe case.expected
    }

    private fun `parses plain-text and non-json inputs`() =
        listOf(
            TestCase(
                input = "",
                structuredEvent = null,
                expected =
                    UnrealLogEvent(
                        time = now,
                        level = LogLevel.Information,
                        message = "",
                        channel = null,
                    ),
            ),
            TestCase(
                input = "LogShaderCompilers: Display: Using Local Shader Compiler with 7 workers.",
                structuredEvent = null,
                expected =
                    UnrealLogEvent(
                        time = now,
                        level = LogLevel.Information,
                        message = "LogShaderCompilers: Display: Using Local Shader Compiler with 7 workers.",
                        channel = null,
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("parses plain-text and non-json inputs")
    fun `parses plain-text and non-json inputs`(case: TestCase) {
        // arrange
        every { jsonLogEventParser.parse(case.input) } returns case.structuredEvent

        // act
        val result = createInstance().parse(case.input)

        // assert
        result shouldBe case.expected
    }

    private fun createInstance() = UnrealLogEventParser(jsonLogEventParser, clock)
}
