package com.jetbrains.teamcity.plugins.framework.common

import com.intellij.execution.configurations.GeneralCommandLine
import jetbrains.buildServer.CommandLineExecutor

class CommandLineRunner {
    companion object {
        private val logger = TeamCityLoggers.get<CommandLineRunner>()
    }

    fun run(commandLine: Command): RunResult? {
        val cmd = GeneralCommandLine().apply {
            exePath = commandLine.executable
            addParameters(commandLine.arguments.map { it })
        }

        val executor = CommandLineExecutor(cmd)
        return executor.runProcess(commandLine.executionTimeoutSeconds)?.let {
            val result = RunResult(
                it.exitCode,
                it.outLines.toList(),
                it.stderr.split("\\r?\\n").toList()
            )

            logger.debug(buildString {
                append("Command ${cmd.commandLineString}} finished. ")
                append("Exit code ${result.exitCode}. ")
                append("Stdout: ${result.standardOutput}. ")
                append("Stderr: ${result.errorOutput}.")
            })

            result
        }
    }

    data class Command(
        val executable: String,
        val arguments: Collection<String>,
        val executionTimeoutSeconds: Int = 10
    )

    data class RunResult(
        val exitCode: Int,
        val standardOutput: Collection<String>,
        val errorOutput: Collection<String>
    )
}
