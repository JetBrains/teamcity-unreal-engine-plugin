package com.jetbrains.teamcity.plugins.framework.common

import java.nio.file.Path

interface Environment {
    val osType: OSType
    val homeDirectory: Path
    val programDataDirectory: Path
}

class OperatingSystemEnvironment : Environment {
    private val os = System.getProperty("os.name").lowercase()

    override val osType: OSType
        get() {
            if (os.startsWith("windows")) {
                return OSType.Windows
            }

            if (os.startsWith("mac")) {
                return OSType.MacOs
            }

            if (os.startsWith("linux")) {
                return OSType.Linux
            }

            return OSType.Unknown
        }

    override val homeDirectory: Path
        get() {
            System.getProperty("user.home")?.let {
                return Path.of(it)
            }

            when (osType) {
                OSType.Windows -> System.getenv("USERPROFILE")
                OSType.Linux,
                OSType.MacOs,
                OSType.Unknown -> System.getenv("HOME")
            }?.let {
                return Path.of(it)
            }

            throw UnsupportedOperationException("Failed to get user home directory")
        }

    override val programDataDirectory: Path
        get() {
            if (osType != OSType.Windows) {
                throw UnsupportedOperationException("There is no such directory as 'ProgramData' defined for non-Windows environments")
            }

            return Path.of(System.getenv("ProgramData"))
        }
}

enum class OSType {
    Unknown,
    Linux,
    MacOs,
    Windows,
}
