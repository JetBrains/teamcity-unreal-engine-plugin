package com.jetbrains.teamcity.plugins.unrealengine.agent.buildgraph

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealBuildContext
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineCommandExecution
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealEngineProgramCommandLine
import com.jetbrains.teamcity.plugins.unrealengine.agent.UnrealToolRegistry
import com.jetbrains.teamcity.plugins.unrealengine.agent.build.log.UnrealEngineProcessListenerFactory
import com.jetbrains.teamcity.plugins.unrealengine.agent.reporting.AutomationTestLogEventHandler
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphCommand
import com.jetbrains.teamcity.plugins.unrealengine.common.raise
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsWatcherEx
import jetbrains.buildServer.agent.runner.ProcessListener

class DistributedExecutor(
    private val toolRegistry: UnrealToolRegistry,
    private val environment: Environment,
    private val artifactsWatcher: ArtifactsWatcherEx,
    private val settingsCreator: DistributedBuildSettingsCreator,
    private val processListenerFactory: UnrealEngineProcessListenerFactory,
) {
    context(_: Raise<GenericError>, context: UnrealBuildContext)
    suspend fun execute(command: BuildGraphCommand): List<UnrealEngineCommandExecution> {
        val settings =
            either {
                settingsCreator.from(context.runnerParameters)
            }.getOrElse {
                raise("Unable to get distributed BuildGraph build settings. Error: ${it.message}")
            }

        return when (settings) {
            is DistributedBuildSettings.SetupBuildSettings -> {
                listOf(
                    setupCommand(settings, command),
                )
            }
            is DistributedBuildSettings.RegularBuildSettings -> {
                listOf(
                    executeCommand(settings, command),
                )
            }
        }
    }

    context(_: Raise<GenericError>, context: UnrealBuildContext)
    private suspend fun setupCommand(
        settings: DistributedBuildSettings.SetupBuildSettings,
        command: BuildGraphCommand,
    ): UnrealEngineCommandExecution {
        ensureSharedDirectoryForBuild(settings.networkShare, settings.compositeBuildId)

        return createUnrealCommandExecution(
            command.toArguments(),
            processListener =
                object : ProcessListener by processListenerFactory.create() {
                    override fun processFinished(exitCode: Int) {
                        if (exitCode == 0) {
                            artifactsWatcher.addNewArtifactsPath(settings.exportedGraphPath)
                            artifactsWatcher.waitForPublishingFinish()
                        }
                    }
                },
        )
    }

    context(_: Raise<GenericError>, context: UnrealBuildContext)
    private suspend fun executeCommand(
        settings: DistributedBuildSettings.RegularBuildSettings,
        command: BuildGraphCommand,
    ): UnrealEngineCommandExecution {
        val sharedDir = ensureSharedDirectoryForBuild(settings.networkShare, settings.compositeBuildId)

        return createUnrealCommandExecution(
            command.toArguments() +
                listOf(
                    "-SharedStorageDir=$sharedDir",
                    "-WriteToSharedStorage",
                ),
            processListenerFactory.create(AutomationTestLogEventHandler(context)),
        )
    }

    context(_: Raise<GenericError>, context: UnrealBuildContext)
    private suspend fun createUnrealCommandExecution(
        arguments: List<String>,
        processListener: ProcessListener,
    ): UnrealEngineCommandExecution =
        UnrealEngineCommandExecution(
            UnrealEngineProgramCommandLine(
                environment,
                context.buildParameters.environmentVariables,
                context.workingDirectory,
                toolRegistry.automationTool(context.runnerParameters).executablePath,
                arguments,
            ),
            processListener,
        )

    context(context: UnrealBuildContext)
    private fun ensureSharedDirectoryForBuild(
        root: String,
        compositeBuildId: String,
    ) = context.createDirectory(context.resolvePath(root, compositeBuildId))
}
