
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProgramCommandLine
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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

        val commandLine = UnrealEngineProgramCommandLine(environmentMock, emptyMap(), "", "", case.arguments)

        // act
        val arguments = commandLine.arguments

        // assert
        arguments shouldBe case.expectedArguments
    }
}
