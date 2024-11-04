
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandCreator
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildConfigurationParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetConfigurationsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetPlatformsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import BuildGraphExecCommandTests.Companion.happyPathCaseParams as buildGraphHappyPathCaseParams

class UnrealCommandCreatorTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
    )

    private fun `test cases`() =
        listOf(
            TestCase(
                runnerParameters =
                    mapOf(
                        UnrealCommandTypeParameter.name to UnrealCommandType.BuildCookRun.name,
                        BuildCookRunProjectPathParameter.name to "some-path",
                        BuildConfigurationParameter.name to "StandaloneGame",
                        UnrealTargetConfigurationsParameter.Standalone.name to "Shipping+Development",
                        UnrealTargetPlatformsParameter.Standalone.name to "Mac+IOS",
                    ),
            ),
            TestCase(
                runnerParameters =
                    mapOf(
                        UnrealCommandTypeParameter.name to UnrealCommandType.BuildGraph.name,
                    ) + buildGraphHappyPathCaseParams,
            ),
        )

    @ParameterizedTest
    @MethodSource("test cases")
    fun `creates a command`(case: TestCase) {
        // act
        val result = either { UnrealCommandCreator().create(case.runnerParameters) }

        // assert
        val command = result.getOrNull()
        command shouldNotBe null
    }

    @Test
    fun `raises an error when the command type is missing`() {
        // act
        val result = either { UnrealCommandCreator().create(mapOf()) }

        // assert
        val errors = result.leftOrNull()
        errors shouldNotBe null
        errors!!.shouldExist {
            it.message.contains("Unreal command type is missing")
        }
    }

    @Test
    fun `should raise an error when the command type is unknown`() {
        val sut = UnrealCommandCreator()

        val result =
            either {
                sut.create(
                    mapOf(
                        UnrealCommandTypeParameter.name to "foo",
                    ),
                )
            }
        val errors = result.leftOrNull()
        errors shouldNotBe null
        errors!!.shouldExist {
            it.message.contains("Unknown Unreal command")
        }
    }
}
