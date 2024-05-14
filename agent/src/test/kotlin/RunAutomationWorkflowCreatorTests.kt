
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealTool
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolRegistry
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolType
import com.jetbrains.teamcity.plugins.unrealengine.agent.WorkflowCreationError
import com.jetbrains.teamcity.plugins.unrealengine.agent.runautomation.RunAutomationWorkflowCreator
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationExecCommandParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.automation.AutomationProjectPathParameter
import io.mockk.called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildProgressLogger
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RunAutomationWorkflowCreatorTests {
    private val buildLoggerMock = mockk<BuildProgressLogger>(relaxed = true)
    private val environmentMock = mockk<Environment> {
        every { osType } returns OSType.MacOs
    }

    private val toolRegistryMock = mockk<UnrealToolRegistry> {
        coEvery {
            with(any<UnrealBuildContext>()) {
                with(any<Raise<WorkflowCreationError>>()) {
                    editor(any())
                }
            }
        } returns UnrealTool("/foo/bar", UnrealToolType.AutomationTool)
    }

    private val buildContext = UnrealBuildContextStub(
        runnerParameters = mapOf(
            AutomationExecCommandParameter.name to AutomationExecCommandParameter.all.name,
            AutomationProjectPathParameter.name to "foo.uproject",
        ),
        build = mockk<AgentRunningBuild> {
            every { buildLogger } returns buildLoggerMock
        },
    )

    @Test
    fun `should contain a single command in the workflow`() = runTest {
        // arrange
        val creator = RunAutomationWorkflowCreator(toolRegistryMock, environmentMock)

        // act
        val workflow = with(buildContext) { either { creator.create() } }.getOrNull()

        // assert
        assertNotNull(workflow)
        assertEquals(1, workflow.commands.count())
    }

    @Test
    fun `do not import test report if no file was generated`() = runTest {
        // arrange
        val creator = RunAutomationWorkflowCreator(toolRegistryMock, environmentMock)

        val buildContext = UnrealBuildContextStub(
            runnerParameters = mapOf(
                AutomationProjectPathParameter.name to "foo.uproject",
            ),
            build = mockk<AgentRunningBuild> {
                every { buildLogger } returns buildLoggerMock
            },
            fileExistsStub = { false },
        )

        // act
        val workflow = with(buildContext) { either { creator.create() } }.getOrNull()
        workflow?.commands?.single()?.processFinished(255)

        // assert
        verify { buildLoggerMock wasNot called }
    }
}
