
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ArchiveDirectoryParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ArchiveOptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchiveOptionsTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedOptions: ArchiveOptions,
        val shouldContainItems: List<String>,
        val shouldNotContainItems: List<String>,
    )

    private val commandExecutionContext = CommandExecutionContextStub()

    companion object {
        @JvmStatic
        fun generateTestCases() =
            listOf(
                TestCase(
                    mapOf(),
                    ArchiveOptions(),
                    listOf("-archive"),
                    listOf("-archivedirectory"),
                ),
                TestCase(
                    mapOf(
                        ArchiveDirectoryParameter.name to "bar",
                    ),
                    ArchiveOptions("bar"),
                    listOf("-archive", "-archivedirectory=foo/bar"),
                    emptyList(),
                ),
            )
    }

    @ParameterizedTest
    @MethodSource("generateTestCases")
    fun `should create options from the given runner parameters`(case: TestCase) {
        // act
        val actual = ArchiveOptions.from(case.runnerParameters)

        // assert
        assertEquals(case.expectedOptions, actual)
    }

    @ParameterizedTest
    @MethodSource("generateTestCases")
    fun `should generate a correct list of arguments`(case: TestCase) {
        // act
        val arguments =
            with(commandExecutionContext) {
                case.expectedOptions.toArguments()
            }

        // assert
        assertTrue(arguments.containsAll(case.shouldContainItems))
        assertTrue(case.shouldNotContainItems.all { !arguments.contains(it) })
    }
}
