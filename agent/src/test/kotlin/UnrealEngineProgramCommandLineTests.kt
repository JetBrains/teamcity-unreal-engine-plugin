
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProgramCommandLine
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.StructuredLogging
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class UnrealEngineProgramCommandLineTests {
    private val environmentMock = mockk<Environment>()

    @BeforeEach
    fun init() {
        clearAllMocks()
    }

    data class TestCase(
        val os: OSType,
        val arguments: List<String>,
        val expectedArguments: List<String>,
    )

    private fun `escapes inner quotes in arguments with spaces on Windows`(): Collection<TestCase> =
        listOf(
            TestCase(
                os = OSType.Windows,
                arguments = listOf("-foo", "-LogCmds=\"LogDerivedDataCache Verbose\"", "-bar"),
                expectedArguments = listOf("-foo", "-LogCmds=\\\"LogDerivedDataCache Verbose\\\"", "-bar"),
            ),
            TestCase(
                os = OSType.Windows,
                arguments = listOf("\"foo bar\""),
                expectedArguments = listOf("\"foo bar\""),
            ),
            TestCase(
                os = OSType.Windows,
                arguments = listOf("\"\""),
                expectedArguments = listOf("\"\""),
            ),
            TestCase(
                os = OSType.Windows,
                arguments = listOf("\"foo \"bar\" \""),
                expectedArguments = listOf("\"foo \\\"bar\\\" \""),
            ),
            TestCase(
                os = OSType.MacOs,
                arguments = listOf("-key=\"value1 value2\""),
                expectedArguments = listOf("-key=\"value1 value2\""),
            ),
            TestCase(
                os = OSType.Linux,
                arguments = listOf("-key=\"value1 value2\""),
                expectedArguments = listOf("-key=\"value1 value2\""),
            ),
        )

    @ParameterizedTest
    @MethodSource("escapes inner quotes in arguments with spaces on Windows")
    fun `escapes inner quotes in arguments with spaces on Windows`(case: TestCase) {
        // arrange
        every { environmentMock.osType } returns case.os

        val commandLine = createCommandLine(arguments = case.arguments)

        // act
        val arguments = commandLine.arguments

        // assert
        arguments shouldBe case.expectedArguments
    }

    @Test
    fun `enables structured build logging by default`() {
        // arrange, act
        val commandLine = createCommandLine()

        // assert
        commandLine.environment shouldContain (StructuredLogging.STRUCTURED_LOGGING_ENV_VAR to "1")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "0", "1"])
    fun `doesn't modify the structured build log variable when provided externally`(value: String) {
        // arrange, act
        val commandLine =
            createCommandLine(
                environmentVariables =
                    mapOf(
                        StructuredLogging.STRUCTURED_LOGGING_ENV_VAR to value,
                    ),
            )

        // assert
        commandLine.environment shouldContain (StructuredLogging.STRUCTURED_LOGGING_ENV_VAR to value)
    }

    private fun createCommandLine(
        environmentVariables: Map<String, String> = emptyMap(),
        arguments: List<String> = emptyList(),
    ) = UnrealEngineProgramCommandLine(environmentMock, environmentVariables, "", "", arguments)
}
