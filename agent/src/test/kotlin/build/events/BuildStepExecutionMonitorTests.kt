package build.events

import com.jetbrains.teamcity.plugins.unrealengine.agent.build.events.BuildStepExecutionMonitor
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEvent
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEventConverter
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.RunnerInternalParameters
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.AgentRuntimeProperties.TEAMCITY_BUILD_STEP_NAME
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test

class BuildStepExecutionMonitorTests {
    private val eventConverter = mockk<AgentBuildEventConverter>()
    private val buildLogLogger = mockk<BuildProgressLogger>()
    private val buildRunnerContext = mockk<BuildRunnerContext>()

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { buildLogLogger.message(any()) } just runs
        with(buildRunnerContext) {
            every { runType } returns UnrealEngineRunner.RUN_TYPE

            withRunnerParameters(
                mapOf(
                    RunnerInternalParameters.BUILD_STEP_NOTIFICATIONS_ENABLED to true.toString(),
                ),
            )

            every { build } returns
                mockk<AgentRunningBuild> {
                    every { buildLogger } returns buildLogLogger
                }

            withStepName("step")
        }
    }

    @Test
    fun `notifies about started build step`() {
        // arrange
        val event = AgentBuildEvent.BuildStepStarted("started")

        buildRunnerContext.withStepName(event.name)
        val expectedBuildLogMessage = event.convertsToBuildLogMessage()

        // act
        BuildStepExecutionMonitor(eventConverter).beforeRunnerStart(buildRunnerContext)

        // assert
        verify { buildLogLogger.message(expectedBuildLogMessage) }
    }

    @Test
    fun `notifies about interrupted step`() {
        // arrange
        val event = AgentBuildEvent.BuildStepInterrupted("interrupted")
        buildRunnerContext.withStepName(event.name)
        val expectedBuildLogMessage = event.convertsToBuildLogMessage()

        // act
        BuildStepExecutionMonitor(eventConverter).runnerFinished(buildRunnerContext, BuildFinishedStatus.INTERRUPTED)

        // assert
        verify { buildLogLogger.message(expectedBuildLogMessage) }
    }

    @Test
    fun `notifies about successful step`() {
        // arrange
        val event = AgentBuildEvent.BuildStepCompleted("success", StepOutcome.Success)
        buildRunnerContext.withStepName(event.name)
        val expectedBuildLogMessage = event.convertsToBuildLogMessage()

        // act
        BuildStepExecutionMonitor(eventConverter).runnerFinished(buildRunnerContext, BuildFinishedStatus.FINISHED_SUCCESS)

        // assert
        verify { buildLogLogger.message(expectedBuildLogMessage) }
    }

    private fun `notifies about failed build step`() =
        listOf(
            BuildFinishedStatus.FINISHED_FAILED,
            BuildFinishedStatus.FINISHED_WITH_PROBLEMS,
        )

    @ParameterizedTest
    @MethodSource("notifies about failed build step")
    fun `notifies about failed build step`(failureStatus: BuildFinishedStatus) {
        // arrange
        val event = AgentBuildEvent.BuildStepCompleted("failed", StepOutcome.Failure)
        buildRunnerContext.withStepName(event.name)
        val expectedBuildLogMessage = event.convertsToBuildLogMessage()

        // act
        BuildStepExecutionMonitor(eventConverter).runnerFinished(buildRunnerContext, failureStatus)

        // assert
        verify { buildLogLogger.message(expectedBuildLogMessage) }
    }

    private fun `does nothing when notifications disabled`() =
        listOf(
            mapOf(
                RunnerInternalParameters.BUILD_STEP_NOTIFICATIONS_ENABLED to false.toString(),
            ),
            mapOf(
                RunnerInternalParameters.BUILD_STEP_NOTIFICATIONS_ENABLED to null,
            ),
            emptyMap(),
        )

    @ParameterizedTest
    @MethodSource("does nothing when notifications disabled")
    fun `does nothing when notifications disabled`(runnerParameters: Map<String, String>) {
        // arrange
        buildRunnerContext.withRunnerParameters(runnerParameters)
        val monitor = BuildStepExecutionMonitor(eventConverter)

        // act
        monitor.beforeRunnerStart(buildRunnerContext)
        monitor.runnerFinished(buildRunnerContext, BuildFinishedStatus.INTERRUPTED)
        monitor.runnerFinished(buildRunnerContext, BuildFinishedStatus.FINISHED_SUCCESS)
        monitor.runnerFinished(buildRunnerContext, BuildFinishedStatus.FINISHED_FAILED)
        monitor.runnerFinished(buildRunnerContext, BuildFinishedStatus.FINISHED_WITH_PROBLEMS)

        // assert
        verify { buildLogLogger wasNot Called }
    }

    private fun BuildRunnerContext.withStepName(name: String) {
        every { configParameters } returns
            mapOf(
                TEAMCITY_BUILD_STEP_NAME to "step",
            )
    }

    private fun BuildRunnerContext.withRunnerParameters(parameters: Map<String, String>) {
        every { runnerParameters } returns parameters
    }

    private fun AgentBuildEvent.convertsToBuildLogMessage(): String {
        val serializedEvent = mapOf("foo" to "bar")
        every { eventConverter.toMap(ofType(this@convertsToBuildLogMessage::class)) } returns serializedEvent
        return ServiceMessage.asString(
            AgentBuildEventConverter.SERVICE_MESSAGE_NAME,
            serializedEvent,
        )
    }
}
