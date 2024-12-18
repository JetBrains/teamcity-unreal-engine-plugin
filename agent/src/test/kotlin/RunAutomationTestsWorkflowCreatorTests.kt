
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealTool
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolRegistry
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolType
import com.jetbrains.teamcity.plugins.unrealengine.agent.automation.tests.RunAutomationTestsWorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealEngineProcessListenerFactory
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsExecCommandParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.tests.AutomationTestsProjectPathParameter
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildParametersMap
import jetbrains.buildServer.agent.BuildProgressLogger
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class RunAutomationTestsWorkflowCreatorTests {
    private val buildLogger = mockk<BuildProgressLogger>(relaxed = true)
    private val buildParameters = mockk<BuildParametersMap>(relaxed = true)
    private val runningBuild = mockk<AgentRunningBuild>()
    private val environment = mockk<Environment>()
    private val processListenerFactory = mockk<UnrealEngineProcessListenerFactory>(relaxed = true)
    private val toolRegistry = mockk<UnrealToolRegistry>()

    @BeforeEach
    fun init() {
        clearAllMocks()

        with(toolRegistry) {
            coEvery {
                with(any<UnrealBuildContext>()) {
                    with(any<Raise<GenericError>>()) {
                        editor(any())
                    }
                }
            } returns UnrealTool("/foo/bar", UnrealToolType.AutomationTool)
        }

        with(environment) {
            every { osType } returns OSType.MacOs
        }

        every { runningBuild.buildLogger } returns buildLogger
    }

    @Test
    fun `contains a single command in the workflow`() =
        runTest {
            // arrange
            val creator = RunAutomationTestsWorkflowCreator(toolRegistry, environment, processListenerFactory)

            // act
            val workflow = with(createContext()) { either { creator.create() } }.getOrNull()

            // assert
            workflow shouldNotBe null
            workflow!!.commands shouldHaveSize 1
        }

    @Test
    fun `does not import test report if no file was generated`() =
        runTest {
            // arrange
            val creator = RunAutomationTestsWorkflowCreator(toolRegistry, environment, processListenerFactory)

            val buildContext =
                createContext(
                    runnerParameters =
                        mapOf(
                            AutomationTestsProjectPathParameter.name to "foo.uproject",
                        ),
                    generateReport = false,
                )

            // act
            val workflow = with(buildContext) { either { creator.create() } }.getOrNull()
            workflow?.commands?.single()?.processFinished(255)

            // assert
            confirmVerified(buildLogger)
        }

    private fun createContext(
        runnerParameters: Map<String, String> =
            mapOf(
                AutomationTestsExecCommandParameter.name to AutomationTestsExecCommandParameter.all.name,
                AutomationTestsProjectPathParameter.name to "foo.uproject",
            ),
        generateReport: Boolean = true,
    ): UnrealBuildContext =
        UnrealBuildContextStub(
            buildParameters = buildParameters,
            runnerParameters = runnerParameters,
            build = runningBuild,
            fileExistsStub = { generateReport },
        )
}
