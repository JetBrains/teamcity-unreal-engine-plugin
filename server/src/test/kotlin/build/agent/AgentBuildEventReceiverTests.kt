package build.agent

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEvent
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEventConverter
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import com.jetbrains.teamcity.plugins.unrealengine.server.build.agent.AgentBuildEventHandler
import com.jetbrains.teamcity.plugins.unrealengine.server.build.agent.AgentBuildEventReceiver
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.isMainNode
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import jetbrains.buildServer.messages.BuildMessage1
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.serverSide.MultiNodesEvents
import jetbrains.buildServer.serverSide.SRunningBuild
import jetbrains.buildServer.serverSide.ServerResponsibility
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentBuildEventReceiverTests {
    private val serviceMessage = mockk<ServiceMessage>()
    private val multiNodesEvents = mockk<MultiNodesEvents>()
    private val build = mockk<SRunningBuild>()
    private val handler = mockk<AgentBuildEventHandler>()
    private val serverResponsibility = mockk<ServerResponsibility>()
    private val eventConverter = mockk<AgentBuildEventConverter>()

    init {
        mockkStatic(ServerResponsibility::isMainNode)
    }

    @BeforeEach
    fun init() {
        clearAllMocks()
        every { serviceMessage.attributes } returns mapOf()
        every { build.buildId } returns 0xC00L

        with(multiNodesEvents) {
            every { publish(any<String>(), any<String>()) } just Runs
            every { subscribe(any(), any()) } just Runs
        }

        coEvery {
            with(any<Raise<Error>>()) {
                handler.handleBuildEvent(any(), any())
            }
        } just Runs
    }

    private fun `agent events`() =
        listOf(
            AgentBuildEvent.BuildStepStarted("started"),
            AgentBuildEvent.BuildStepInterrupted("interrupted"),
            AgentBuildEvent.BuildStepCompleted("failed", StepOutcome.Failure),
            AgentBuildEvent.BuildStepCompleted("succeeded", StepOutcome.Success),
        )

    @ParameterizedTest
    @MethodSource("agent events")
    fun `publishes multi-node event when not on main node`(event: AgentBuildEvent) {
        // arrange
        eventConverter.convertsMessageTo(serviceMessage, event)

        every { serverResponsibility.isMainNode() } returns false

        // act
        createInstance().act()

        // assert
        verify { multiNodesEvents.publish(any<String>(), any<String>()) }
        verify { handler wasNot Called }
    }

    @ParameterizedTest
    @MethodSource("agent events")
    fun `does not publish multi-node event for when already on main node`(event: AgentBuildEvent) =
        runTest {
            // arrange
            eventConverter.convertsMessageTo(serviceMessage, event)

            every { serverResponsibility.isMainNode() } returns true
            // act
            createInstance().act()

            // assert
            verify(exactly = 0) { multiNodesEvents.publish(any<String>(), any<String>()) }
            coVerify {
                with(any<Raise<Error>>()) {
                    handler.handleBuildEvent(0xC00L, event)
                }
            }
        }

    private fun AgentBuildEventConverter.convertsMessageTo(
        serviceMessage: ServiceMessage,
        event: AgentBuildEvent,
    ) {
        every { with(any<Raise<Error>>()) { fromMap(serviceMessage.attributes) } } returns event
    }

    private fun getMultiNodeEvents() =
        mockk<MultiNodesEvents> {
            every { publish(any<String>(), any<String>()) } just Runs
            every { subscribe(any(), any()) } just Runs
        }

    private fun createInstance() =
        AgentBuildEventReceiver(
            listOf(handler),
            eventConverter,
            multiNodesEvents,
            serverResponsibility,
        )

    private fun AgentBuildEventReceiver.act(): List<BuildMessage1> = translate(build, mockk(), serviceMessage)
}
