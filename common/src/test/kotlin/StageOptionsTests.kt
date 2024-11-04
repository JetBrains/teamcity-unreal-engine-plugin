
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CompressedContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.PrerequisitesParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.StageOptions
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.StagingDirectoryParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UsePakParameter
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class StageOptionsTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedOptions: StageOptions,
        val shouldContainItems: List<String>,
        val shouldNotContainItems: List<String>,
    )

    private val workingDir = "FOO"

    private val commandExecutionContext =
        CommandExecutionContextStub(
            workingDirectory = workingDir,
        )

    private fun `test cases`() =
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
                        "-stagingdirectory=$workingDir/some-dir/some-sub-dir",
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

    @ParameterizedTest
    @MethodSource("test cases")
    fun `creates options from the given runner parameters`(case: TestCase) {
        // act
        val options = StageOptions.from(case.runnerParameters)

        // assert
        options shouldBe case.expectedOptions
    }

    @ParameterizedTest
    @MethodSource("test cases")
    fun `generates a correct list of arguments`(case: TestCase) {
        // act
        val arguments =
            with(commandExecutionContext) {
                case.expectedOptions.toArguments()
            }

        // assert
        arguments shouldContainAll case.shouldContainItems
        if (case.shouldNotContainItems.isNotEmpty()) {
            arguments shouldNotContainAnyOf case.shouldNotContainItems
        }
    }
}
