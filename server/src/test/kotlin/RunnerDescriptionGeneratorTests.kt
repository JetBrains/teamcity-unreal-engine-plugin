
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealCommandType
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.commands.AutomationCommandNameParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.BuildCookRunProjectPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphScriptPathParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphTargetNodeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.EngineDetectionModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealCommandTypeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineIdentifierParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.parameters.UnrealEngineRootParameter
import com.jetbrains.teamcity.plugins.unrealengine.server.runner.RunnerDescriptionGenerator
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class RunnerDescriptionGeneratorTests {
    data class TestCase(
        val runnerParameters: Map<String, String>,
        val expectedDescription: String,
    )

    private fun `generates a proper description when the parameters are correct`(): Collection<TestCase> =
        buildList {
            val buildCookRun =
                TestCase(
                    runnerParameters =
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.manual.name,
                            UnrealEngineRootParameter.name to "/foo/UE_5.2",
                            UnrealCommandTypeParameter.name to UnrealCommandType.BuildCookRun.name,
                            BuildCookRunProjectPathParameter.name to "foo/bar.uproject",
                        ),
                    expectedDescription =
                        "Command: BuildCookRun, Project: foo/bar.uproject, " +
                            "Engine detection mode: Manual, Engine Root path: /foo/UE_5.2",
                )

            val buildGraph =
                TestCase(
                    runnerParameters =
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.automatic.name,
                            UnrealEngineIdentifierParameter.name to "5.3",
                            UnrealCommandTypeParameter.name to UnrealCommandType.BuildGraph.name,
                            BuildGraphScriptPathParameter.name to "foo/BuildGraph.xml",
                            BuildGraphTargetNodeParameter.name to "FooBar",
                            BuildGraphModeParameter.name to BuildGraphModeParameter.distributed.name,
                        ),
                    expectedDescription =
                        "Command: BuildGraph, Script path: foo/BuildGraph.xml, " +
                            "Target: FooBar, Mode: Distributed, Engine detection mode: Auto, Engine identifier: 5.3",
                )

            val runAutomationTests =
                TestCase(
                    runnerParameters =
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.automatic.name,
                            UnrealEngineIdentifierParameter.name to "5.3",
                            UnrealCommandTypeParameter.name to UnrealCommandType.RunAutomationTests.value,
                            AutomationTestsProjectPathParameter.name to "bar/baz.uproject",
                        ),
                    expectedDescription =
                        "Command: RunAutomationTests, Project: bar/baz.uproject, " +
                            "Engine detection mode: Auto, Engine identifier: 5.3",
                )

            val runAutomationCommand =
                TestCase(
                    runnerParameters =
                        mapOf(
                            EngineDetectionModeParameter.name to EngineDetectionModeParameter.manual.name,
                            UnrealEngineRootParameter.name to "./engine-root",
                            UnrealCommandTypeParameter.name to UnrealCommandType.RunAutomationCommand.value,
                            AutomationCommandNameParameter.name to "AnalyzeThirdPartyLibs",
                        ),
                    expectedDescription =
                        "Command: RunAutomationCommand, Name: AnalyzeThirdPartyLibs, " +
                            "Engine detection mode: Manual, Engine Root path: ./engine-root",
                )

            add(buildCookRun)
            add(buildGraph)
            add(runAutomationTests)
            add(runAutomationCommand)
        }

    @ParameterizedTest
    @MethodSource("generates a proper description when the parameters are correct")
    fun `generates a proper description when the parameters are correct`(case: TestCase) {
        // arrange
        val generator = RunnerDescriptionGenerator()

        // act
        val result = generator.generate(case.runnerParameters)

        // assert
        result shouldBe case.expectedDescription
    }
}
