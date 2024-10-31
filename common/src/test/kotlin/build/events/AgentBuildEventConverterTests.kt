package build.events
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEvent
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEvent.*
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEventConverter
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentBuildEventConverterTests {
    data class TestCase(
        val event: AgentBuildEvent,
        val map: Map<String, String>,
    )

    private fun `test cases`(): List<TestCase> =
        listOf(
            TestCase(
                BuildStepStarted("Compile Win64 Game"),
                mapOf(
                    "type" to "build-step.started",
                    "name" to "Compile Win64 Game",
                ),
            ),
            TestCase(
                BuildStepCompleted("Compile Win64 Game", StepOutcome.Success),
                mapOf(
                    "type" to "build-step.completed",
                    "name" to "Compile Win64 Game",
                    "outcome" to "success",
                ),
            ),
            TestCase(
                BuildStepCompleted("Compile Win64 Game", StepOutcome.Failure),
                mapOf(
                    "type" to "build-step.completed",
                    "name" to "Compile Win64 Game",
                    "outcome" to "failure",
                ),
            ),
            TestCase(
                BuildStepInterrupted("Compile Win64 Game"),
                mapOf(
                    "type" to "build-step.interrupted",
                    "name" to "Compile Win64 Game",
                ),
            ),
        )

    @ParameterizedTest
    @MethodSource("test cases")
    fun `converts to map`(case: TestCase) {
        // arrange, act
        val result = AgentBuildEventConverter().toMap(case.event)

        // assert
        result shouldBeEqual case.map
    }

    @ParameterizedTest
    @MethodSource("test cases")
    fun `converts from map`(case: TestCase) {
        // arrange, act
        val result = either { AgentBuildEventConverter().fromMap(case.map) }.getOrNull()

        // assert
        result shouldNotBe null
        result?.shouldBeEqual(case.event)
    }
}
