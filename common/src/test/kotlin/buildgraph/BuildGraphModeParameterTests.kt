package buildgraph
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphMode
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.PostBadgesFromGraphParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.UgsMetadataServerUrlParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BuildGraphModeParameterTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
        val mode: BuildGraphMode? = null,
    )

    private fun `parses single machine mode cases`() =
        listOf(
            TestCase(
                runnerParameters = mapOf(),
                mode = BuildGraphMode.SingleMachine,
            ),
            TestCase(
                runnerParameters =
                    mapOf(
                        BuildGraphModeParameter.name to "SingleMachine",
                    ),
                mode = BuildGraphMode.SingleMachine,
            ),
        )

    @ParameterizedTest
    @MethodSource("parses single machine mode cases")
    fun `parses single machine mode`(case: TestCase) {
        // act
        val result = act(case.runnerParameters).getOrNull()

        // assert
        result shouldNotBe null
        result shouldBe case.mode
    }

    private fun `parses distributed mode cases`() =
        listOf(
            TestCase(
                runnerParameters =
                    mapOf(
                        BuildGraphModeParameter.name to "Distributed",
                    ),
                mode = BuildGraphMode.Distributed(),
            ),
            TestCase(
                runnerParameters =
                    mapOf(
                        BuildGraphModeParameter.name to "Distributed",
                        PostBadgesFromGraphParameter.name to "false",
                    ),
                mode = BuildGraphMode.Distributed(),
            ),
            TestCase(
                runnerParameters =
                    mapOf(
                        BuildGraphModeParameter.name to "Distributed",
                        PostBadgesFromGraphParameter.name to "true",
                        UgsMetadataServerUrlParameter.name to "http://localhost:1111/ugs-metadata-server",
                    ),
                mode = BuildGraphMode.Distributed(UgsMetadataServerUrl("http://localhost:1111/ugs-metadata-server")),
            ),
        )

    @ParameterizedTest
    @MethodSource("parses distributed mode cases")
    fun `parses distributed mode`(case: TestCase) {
        // act
        val result = act(case.runnerParameters).getOrNull()

        // assert
        result shouldNotBe null
        result shouldBe case.mode
    }

    private fun `raises error when runner parameters are invalid cases`() =
        listOf(
            TestCase(
                runnerParameters =
                    mapOf(
                        BuildGraphModeParameter.name to "Distributed",
                        PostBadgesFromGraphParameter.name to "true",
                    ),
            ),
            TestCase(
                runnerParameters =
                    mapOf(
                        BuildGraphModeParameter.name to "Distributed",
                        PostBadgesFromGraphParameter.name to "true",
                        UgsMetadataServerUrlParameter.name to "             ",
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("raises error when runner parameters are invalid cases")
    fun `raises error when runner parameters are invalid`(case: TestCase) {
        // act
        val result = act(case.runnerParameters).leftOrNull()

        // assert
        result shouldNotBe null
    }

    private fun act(runnerParameters: Map<String, String>) = either { BuildGraphModeParameter.parse(runnerParameters) }
}
