package com.jetbrains.teamcity.plugins.unrealengine.agent

import jetbrains.buildServer.agent.runner.CommandExecution
import jetbrains.buildServer.agent.runner.ProcessListener
import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.agent.runner.TerminationAction
import java.io.File

sealed interface UnrealEngineCommandState {
    data object NotStarted : UnrealEngineCommandState
    data object Running : UnrealEngineCommandState
    data class Finished(val exitCode: Int) : UnrealEngineCommandState
}

class UnrealEngineCommandExecution(
    private val commandLine: ProgramCommandLine,
    private val listener: ProcessListener,
) : CommandExecution {
    var state: UnrealEngineCommandState = UnrealEngineCommandState.NotStarted
        private set

    override fun onStandardOutput(text: String) = listener.onStandardOutput(text)

    override fun onErrorOutput(text: String) = listener.onErrorOutput(text)

    override fun processStarted(programCommandLine: String, workingDirectory: File) {
        state = UnrealEngineCommandState.Running
        listener.processStarted(programCommandLine, workingDirectory)
    }

    override fun processFinished(exitCode: Int) {
        state = UnrealEngineCommandState.Finished(exitCode)
        listener.processFinished(exitCode)
    }

    override fun makeProgramCommandLine() = commandLine

    override fun beforeProcessStarted() = Unit

    override fun interruptRequested() = TerminationAction.KILL_PROCESS_TREE

    override fun isCommandLineLoggingEnabled() = true
}
