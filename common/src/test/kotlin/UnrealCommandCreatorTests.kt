
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandCreator
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildConfigurationParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetConfigurationsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnrealTargetPlatformsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import BuildGraphExecCommandTests.Companion.happyPathCaseParams as buildGraphHappyPathCaseParams

class UnrealCommandCreatorTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
    )

    companion object {
        @JvmStatic
        fun generateTestCases() =
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
    }

    @ParameterizedTest
    @MethodSource("generateTestCases")
    fun `should create a command`(case: TestCase) {
        val sut = UnrealCommandCreator()

        val result = either { sut.create(case.runnerParameters) }
        val command = result.getOrNull()
        assertNotNull(command)
    }

    @Test
    fun `should raise an error when the command type is missing`() {
        val sut = UnrealCommandCreator()

        val result = either { sut.create(mapOf()) }
        val errors = result.leftOrNull()
        assertNotNull(errors)
        assertTrue {
            errors.any { it.message.contains("Unreal command type is missing") }
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
        assertNotNull(errors)
        assertTrue {
            errors.any { it.message.contains("Unknown Unreal command") }
        }
    }
}
