package com.jetbrains.teamcity.plugins.unrealengine.server

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

data class EventBusConfig(
    val name: String,
    val workerCount: Int,
    val workerBufferSize: Int,
)

interface EventBusConsumer<T> {
    suspend fun consume(event: T)
}

class EventBus<T>(
    private val config: EventBusConfig,
    private val scope: CoroutineScope,
    private val consumers: List<EventBusConsumer<T>>,
    private val partitioner: (T) -> Int,
    private val onBufferOverflow: ((T) -> Unit)? = null,
) {
    private val logger = UnrealPluginLoggers.get<EventBus<T>>()

    private val workerChannels: List<Channel<T>> =
        (0..<config.workerCount)
            .map {
                Channel(
                    capacity = config.workerBufferSize,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST,
                    onUndeliveredElement = { droppedElement ->
                        val droppedElementKey = partitioner(droppedElement)
                        logger.warn(
                            "Worker $it dropped an element with key $droppedElementKey due to buffer overflow, consider tuning EventBus settings",
                        )
                        onBufferOverflow?.invoke(droppedElement)
                    },
                )
            }

    init {
        logger.debug(
            "Bus \"${config.name}\" initialized with ${config.workerCount} internal workers, each with a buffer size of ${config.workerBufferSize}",
        )

        workerChannels.forEachIndexed { workerNumber, channel ->
            scope.launch {
                for (event in channel) {
                    logger.debug("Worker $workerNumber from bus \"${config.name}\" received a new event")
                    consumers.forEach { consumer ->
                        consumer
                            .runCatching {
                                consume(event)
                            }.onFailure {
                                logger.warn(
                                    "An error occurred while processing an event in worker $workerNumber from bus \"${config.name}\"",
                                    it,
                                )
                            }
                    }
                }
            }
        }
    }

    suspend fun dispatch(event: T) {
        val partitionKey = partitioner(event)
        val workerNumber = partitionKey % workerChannels.size
        logger.debug("Dispatching new event to bus \"${config.name}\", partition key = $partitionKey, assigned worker = $workerNumber")
        workerChannels[workerNumber].send(event)
    }
}
