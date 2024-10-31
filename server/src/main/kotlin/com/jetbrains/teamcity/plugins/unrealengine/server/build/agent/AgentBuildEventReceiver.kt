package com.jetbrains.teamcity.plugins.unrealengine.server.build.agent

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.forEachAccumulating
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.JsonEncoder
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEvent
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.AgentBuildEventConverter
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.isMainNode
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.logError
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.logResult
import jetbrains.buildServer.messages.BuildMessage1
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTranslator
import jetbrains.buildServer.serverSide.MultiNodesEvents
import jetbrains.buildServer.serverSide.SRunningBuild
import jetbrains.buildServer.serverSide.ServerResponsibility
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class AgentBuildEventReceiver(
    private val handlers: Collection<AgentBuildEventHandler>,
    private val eventConverter: AgentBuildEventConverter,
    private val multiNodesEvents: MultiNodesEvents,
    private val serverResponsibility: ServerResponsibility,
) : ServiceMessageTranslator {
    companion object {
        private val logger = TeamCityLoggers.server<AgentBuildEventReceiver>()
    }

    private val json = JsonEncoder.instance

    init {
        multiNodesEvents.subscribe(UnrealMultiNodeAgentBuildEvent.NAME, ::consumeUnrealMultiNodeEvent)
    }

    override fun translate(
        build: SRunningBuild,
        buildMessage: BuildMessage1,
        serviceMessage: ServiceMessage,
    ): MutableList<BuildMessage1> {
        val result = either { processServiceMessage(build, serviceMessage) }
        logger.logResult(
            result,
            context = "Service message processing",
            successMessage = "Received service message processed successfully",
        )

        return mutableListOf()
    }

    context(Raise<Error>)
    private fun processServiceMessage(
        build: SRunningBuild,
        serviceMessage: ServiceMessage,
    ) = runBlocking {
        val event = eventConverter.fromMap(serviceMessage.attributes)

        if (!serverResponsibility.isMainNode()) {
            logger.debug(
                """
                This is a secondary node. Skipping processing here and sending a multi-node event instead
                """.trimIndent(),
            )
            val multiNodeEvent = UnrealMultiNodeAgentBuildEvent(build.buildId, event)
            multiNodesEvents.publish(UnrealMultiNodeAgentBuildEvent.NAME, json.encodeToString(multiNodeEvent))
        } else {
            callHandlers(build.buildId, event)
        }
    }

    private fun consumeUnrealMultiNodeEvent(event: MultiNodesEvents.Event) =
        runBlocking {
            val result = either { processMultiNodeEvent(event) }
            logger.logResult(
                result,
                context = "Unreal multi-node event processing",
                successMessage = "Unreal multi-node event consumed successfully",
            )
        }

    context(Raise<Error>)
    private suspend fun processMultiNodeEvent(event: MultiNodesEvents.Event) {
        val serializedEvent = ensureNotNull(event.stringArg, "Multi-node event is missing a payload")
        val multiNodeEvent = json.decodeFromString<UnrealMultiNodeAgentBuildEvent>(serializedEvent)

        if (!serverResponsibility.isMainNode()) {
            logger.debug("Ignoring received multi-node event since this is a secondary node")
            return
        }

        callHandlers(multiNodeEvent.buildId, multiNodeEvent.event)
    }

    context(Raise<Error>)
    private suspend fun callHandlers(
        buildId: Long,
        agentEvent: AgentBuildEvent,
    ) {
        either {
            forEachAccumulating(handlers) {
                it.handleBuildEvent(buildId, agentEvent)
            }
        }.mapLeft {
            logAllErrors(it)
            GenericError("Not all handlers completed successfully")
        }
    }

    private fun logAllErrors(errors: NonEmptyList<Error>) {
        for (error in errors) {
            when (error) {
                is GenericError -> logger.logError(error, "There was an error handling an event in one of the handlers: ")
                else -> logger.error("An unexpected error occurred in one of the handlers")
            }
        }
    }

    override fun getServiceMessageName() = AgentBuildEventConverter.SERVICE_MESSAGE_NAME
}

@Serializable
private data class UnrealMultiNodeAgentBuildEvent(
    val buildId: Long,
    val event: AgentBuildEvent,
) {
    companion object {
        const val NAME = "unreal-engine.agent-build-event"
    }
}
