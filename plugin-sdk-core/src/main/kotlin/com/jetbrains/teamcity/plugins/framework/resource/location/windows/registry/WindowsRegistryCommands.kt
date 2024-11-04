package com.jetbrains.teamcity.plugins.framework.resource.location.windows.registry

import com.jetbrains.teamcity.plugins.framework.common.CommandLineRunner

internal class WindowsRegistryCommands {
    companion object {
        fun query(keyName: String) = CommandLineRunner.Command(
            "reg",
            listOf(
                "query",
                keyName
            )
        )
    }
}
