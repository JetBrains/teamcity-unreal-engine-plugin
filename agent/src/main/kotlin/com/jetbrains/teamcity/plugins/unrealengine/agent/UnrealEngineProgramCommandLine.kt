package com.jetbrains.teamcity.plugins.unrealengine.agent

import com.jetbrains.teamcity.plugins.framework.common.Environment
import com.jetbrains.teamcity.plugins.framework.common.OSType
import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine
import jetbrains.buildServer.util.StringUtil

class UnrealEngineProgramCommandLine
    private constructor(
        private val environment: Environment,
        private val commandLine: ProgramCommandLine,
    ) : ProgramCommandLine by commandLine {
        constructor(
            environment: Environment,
            envVariables: Map<String, String>,
            workingDirectory: String,
            executablePath: String,
            arguments: List<String>,
        ) : this(
            environment,
            SimpleProgramCommandLine(envVariables, workingDirectory, executablePath, arguments),
        )

        override fun getArguments(): MutableList<String> {
            val onWindows = environment.osType == OSType.Windows
            return commandLine.arguments
                .map {
                    if (onWindows) {
                        it.escapeInnerQuotes()
                    } else {
                        it
                    }
                }
                .toMutableList()
        }

        private fun String.escapeInnerQuotes(): String {
            if (any { char -> char.isWhitespace() }) {
                return if (isQuoted()) {
                    "\"" + StringUtil.escapeQuotes(substring(1, length - 1)) + "\""
                } else {
                    StringUtil.escapeQuotes(this)
                }
            }

            return this
        }

        private fun String.isQuoted() = length > 1 && this[0] == '"' && this[length - 1] == '"'
    }
