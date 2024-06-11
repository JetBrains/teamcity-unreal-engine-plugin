
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CompressedContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.PrerequisitesParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.StageOptions
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.StagingDirectoryParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UsePakParameter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StageOptionsTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedOptions: StageOptions,
        val shouldContainItems: List<String>,
        val shouldNotContainItems: List<String>,
    )

    private val commandExecutionContext =
        CommandExecutionContextStub(
            workingDirectory = WORKING_DIR,
        )

    companion object {
        const val WORKING_DIR = "FOO"

        @JvmStatic
        fun generateTestCases() =
            listOf(
                TestCase(
                    runnerParameters =
                        mapOf(
                            StagingDirectoryParameter.name to "some-dir/some-sub-dir",
                            UsePakParameter.name to true.toString(),
                            CompressedContentParameter.name to true.toString(),
                            PrerequisitesParameter.name to true.toString(),
                        ),
                    expectedOptions =
                        StageOptions(
                            stagingDirectory = "some-dir/some-sub-dir",
                            usePak = true,
                            compressContent = true,
                            installPrerequisites = true,
                        ),
                    shouldContainItems =
                        listOf(
                            "-stage",
                            "-stagingdirectory=$WORKING_DIR/some-dir/some-sub-dir",
                            "-pak",
                            "-compressed",
                            "-prereqs",
                        ),
                    shouldNotContainItems = emptyList(),
                ),
                TestCase(
                    runnerParameters = mapOf(),
                    expectedOptions = StageOptions(),
                    shouldContainItems = listOf("-stage"),
                    shouldNotContainItems = listOf("-stagingdirectory", "-pak", "-compressed", "-prereqs"),
                ),
            )
    }

    @ParameterizedTest
    @MethodSource("generateTestCases")
    fun `should create options from the given runner parameters`(case: TestCase) {
        val actual = StageOptions.from(case.runnerParameters)

        assertEquals(case.expectedOptions, actual)
    }

    @ParameterizedTest
    @MethodSource("generateTestCases")
    fun `should generate a correct list of arguments`(case: TestCase) {
        val arguments =
            with(commandExecutionContext) {
                case.expectedOptions.toArguments()
            }

        assertTrue(arguments.containsAll(case.shouldContainItems))
        assertTrue(case.shouldNotContainItems.all { !arguments.contains(it) })
    }
}
