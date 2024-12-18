package build.log

import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.LogLevel
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealJsonLogEvent
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealJsonLogEventParser
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant

class UnrealJsonLogEventParserTests {
    data class TestCase(
        val string: String,
        val expected: UnrealJsonLogEvent?,
    )

    private fun `parses valid JSON events`() =
        listOf(
            TestCase(
                string =
                    """
                    {
                      "time": "2028-10-05T09:46:52Z",
                      "level": "Information",
                      "message": "Parsing command line: BuildCookRun ...",
                      "properties": {
                        "CommandLine": "BuildCookRun ..."
                      }
                    }
                    """.trimIndent(),
                expected =
                    UnrealJsonLogEvent(
                        time = Instant.parse("2028-10-05T09:46:52Z"),
                        level = LogLevel.Information,
                        message = "Parsing command line: BuildCookRun ...",
                        properties =
                            mapOf(
                                "CommandLine" to JsonPrimitive("BuildCookRun ..."),
                            ),
                    ),
            ),
            TestCase(
                string =
                    """
                    {
                      "time": "2027-11-05T09:54:59.018Z",
                      "level": "Error",
                      "message": "LogPython: Error:   File",
                      "properties": {
                        "_channel": {
                          "${'$'}type": "Channel",
                          "${'$'}text": "LogPython"
                        },
                        "_severity": {
                          "${'$'}type": "Severity",
                          "${'$'}text": "Error"
                        }
                      }
                    }
                    """.trimIndent(),
                expected =
                    UnrealJsonLogEvent(
                        time = Instant.parse("2027-11-05T09:54:59.018Z"),
                        level = LogLevel.Error,
                        message = "LogPython: Error:   File",
                        properties =
                            mapOf(
                                "_channel" to
                                    JsonObject(
                                        mapOf(
                                            "\$type" to JsonPrimitive("Channel"),
                                            "\$text" to JsonPrimitive("LogPython"),
                                        ),
                                    ),
                                "_severity" to
                                    JsonObject(
                                        mapOf(
                                            "\$type" to JsonPrimitive("Severity"),
                                            "\$text" to JsonPrimitive("Error"),
                                        ),
                                    ),
                            ),
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("parses valid JSON events")
    fun `parses valid JSON events`(case: TestCase) {
        // arrange
        val parser = UnrealJsonLogEventParser()

        // act
        val result = parser.parse(case.string)

        // assert
        result shouldBe case.expected
    }

    private fun `fails to parse invalid or non-JSON strings`() =
        listOf(
            // Completely invalid JSON string
            TestCase(
                string = "This is not JSON",
                expected = null,
            ),
            // Missing closing brace
            TestCase(
                string =
                    """
                    {
                      "time": "2027-11-05T09:54:59.018Z",
                      "level": "Information",
                      "message": "This JSON is incomplete"
                    """.trimIndent(),
                expected = null,
            ),
            // Invalid JSON type for "time" (expected Instant, got integer)
            TestCase(
                string =
                    """
                    {
                      "time": 12345,
                      "level": "Information",
                      "message": "Invalid time format",
                      "properties": {}
                    }
                    """.trimIndent(),
                expected = null,
            ),
            // Missing required fields (e.g., "message")
            TestCase(
                string =
                    """
                    {
                      "time": "2027-11-05T09:54:59.018Z",
                      "level": "Information",
                      "properties": {}
                    }
                    """.trimIndent(),
                expected = null,
            ),
            // Malformed JSON (random text in middle of JSON)
            TestCase(
                string =
                    """
                    {
                      "time": "2027-11-05T09:54:59.018Z",
                      "level": "Information",
                      randomTextHere
                      "message": "Malformed JSON",
                      "properties": {}
                    }
                    """.trimIndent(),
                expected = null,
            ),
        )

    @ParameterizedTest
    @MethodSource("fails to parse invalid or non-JSON strings")
    fun `fails to parse invalid or non-JSON strings`(case: TestCase) {
        // arrange
        val parser = UnrealJsonLogEventParser()

        // act
        val result = parser.parse(case.string)

        // assert
        result shouldBe null
    }
}
