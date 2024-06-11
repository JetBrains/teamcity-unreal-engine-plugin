
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProgramCommandLine
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class UnrealEngineProgramCommandLineTests {
    private val environmentMock = mockk<Environment>()

    data class ArgumentsTestCase(
        val os: OSType,
        val arguments: List<String>,
        val expectedArguments: List<String>,
    )

    companion object {
        @JvmStatic
        fun generateArgumentsTestCases(): Collection<ArgumentsTestCase> =
            listOf(
                ArgumentsTestCase(
                    OSType.Windows,
                    listOf("-foo", "-LogCmds=\"LogDerivedDataCache Verbose\"", "-bar"),
                    listOf("-foo", "-LogCmds=\\\"LogDerivedDataCache Verbose\\\"", "-bar"),
                ),
                ArgumentsTestCase(
                    OSType.Windows,
                    listOf("\"foo bar\""),
                    listOf("\"foo bar\""),
                ),
                ArgumentsTestCase(
                    OSType.Windows,
                    listOf("\"\""),
                    listOf("\"\""),
                ),
                ArgumentsTestCase(
                    OSType.Windows,
                    listOf("\"foo \"bar\" \""),
                    listOf("\"foo \\\"bar\\\" \""),
                ),
                ArgumentsTestCase(
                    OSType.MacOs,
                    listOf("-key=\"value1 value2\""),
                    listOf("-key=\"value1 value2\""),
                ),
                ArgumentsTestCase(
                    OSType.Linux,
                    listOf("-key=\"value1 value2\""),
                    listOf("-key=\"value1 value2\""),
                ),
            )
    }

    @ParameterizedTest
    @MethodSource("generateArgumentsTestCases")
    fun `should escape inner quotes in arguments with spaces on Windows`(case: ArgumentsTestCase) {
        // arrange
        every { environmentMock.osType } returns case.os

        val commandLine = UnrealEngineProgramCommandLine(environmentMock, emptyMap(), "", "", case.arguments)

        // act
        val arguments = commandLine.arguments

        // assert
        assertEquals(case.expectedArguments, arguments)
    }
}
