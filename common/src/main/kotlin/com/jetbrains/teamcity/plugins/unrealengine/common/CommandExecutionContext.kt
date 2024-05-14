package com.jetbrains.teamcity.plugins.unrealengine.common

interface FileSystemContext {
    val workingDirectory: String
    val agentTempDirectory: String
    fun concatPaths(root: String, path: String): String
    fun fileExists(path: String): Boolean
    fun isAbsolute(path: String): Boolean
    fun createDirectory(root: String, vararg parts: String): String
}

interface CommandExecutionContext : FileSystemContext
