
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphMode
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphScriptPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphTargetNodeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineIdentifierParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineRootParameter
import com.jetbrains.teamcity.plugins.unrealengine.server.runner.RunnerDescriptionGenerator
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class RunnerDescriptionGeneratorTests {
    companion object {
        @JvmStatic
        fun generateHappyPathTestCases(): Collection<TestCase> =
            buildList {
                val buildCookRun =
                    TestCase(
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.manual.name,
                            UnrealEngineRootParameter.name to "/foo/UE_5.2",
                            UnrealCommandTypeParameter.name to UnrealCommandType.BuildCookRun.name,
                            BuildCookRunProjectPathParameter.name to "foo/bar.uproject",
                        ),
                        "Command: BuildCookRun, Project: foo/bar.uproject, " +
                            "Engine detection mode: Manual, Engine Root path: /foo/UE_5.2",
                    )

                val buildGraph =
                    TestCase(
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.automatic.name,
                            UnrealEngineIdentifierParameter.name to "5.3",
                            UnrealCommandTypeParameter.name to UnrealCommandType.BuildGraph.name,
                            BuildGraphScriptPathParameter.name to "foo/BuildGraph.xml",
                            BuildGraphTargetNodeParameter.name to "FooBar",
                            BuildGraphModeParameter.name to BuildGraphMode.Distributed.name,
                        ),
                        "Command: BuildGraph, Script path: foo/BuildGraph.xml, " +
                            "Target: FooBar, Mode: Distributed, Engine detection mode: Auto, Engine identifier: 5.3",
                    )

                val runAutomation =
                    TestCase(
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.automatic.name,
                            UnrealEngineIdentifierParameter.name to "5.3",
                            UnrealCommandTypeParameter.name to UnrealCommandType.RunAutomation.name,
                            AutomationProjectPathParameter.name to "bar/baz.uproject",
                        ),
                        "Command: RunAutomation, Project: bar/baz.uproject, " +
                            "Engine detection mode: Auto, Engine identifier: 5.3",
                    )

                add(buildCookRun)
                add(buildGraph)
                add(runAutomation)
            }
    }

    data class TestCase(
        val parameters: Map<String, String>,
        val expectedDescription: String,
    )

    @ParameterizedTest
    @MethodSource("generateHappyPathTestCases")
    fun `should generate a proper description when the parameters are correct`(case: TestCase) {
        // arrange
        val generator = RunnerDescriptionGenerator()

        // act
        val result = generator.generate(case.parameters)

        // assert
        assertEquals(case.expectedDescription, result)
    }
}
