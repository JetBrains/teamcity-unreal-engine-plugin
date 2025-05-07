
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ArchiveDirectoryParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ArchiveOptions
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ArchiveOptionsTests {
    private val context = createTestCommandExecutionContext()

    @BeforeEach
    fun init() {
        clearAllMocks()
        setupTestCommandExecutionContext(context)
    }

    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedOptions: ArchiveOptions,
        val expectedArguments: List<String>,
    )

    private fun `test cases`() =
        listOf(
            TestCase(
                runnerParameters = mapOf(),
                expectedOptions = ArchiveOptions(),
                expectedArguments = listOf("-archive"),
            ),
            TestCase(
                runnerParameters =
                    mapOf(
                        ArchiveDirectoryParameter.name to "bar",
                    ),
                expectedOptions = ArchiveOptions("bar"),
                expectedArguments = listOf("-archive", "-archivedirectory=foo/bar"),
            ),
        )

    @ParameterizedTest
    @MethodSource("test cases")
    fun `creates options from the given runner parameters`(case: TestCase) {
        // act
        val actual = ArchiveOptions.from(case.runnerParameters)

        // assert
        actual shouldBe case.expectedOptions
    }

    @ParameterizedTest
    @MethodSource("test cases")
    fun `generates a correct list of arguments`(case: TestCase) {
        // act
        val arguments =
            with(context) {
                case.expectedOptions.toArguments()
            }

        // assert
        arguments shouldContainExactly case.expectedArguments
    }
}
