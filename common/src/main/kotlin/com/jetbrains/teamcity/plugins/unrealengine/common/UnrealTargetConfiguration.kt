package com.jetbrains.teamcity.plugins.unrealengine.common

@JvmInline
value class UnrealTargetConfiguration(
    val value: String,
) {
    companion object {
        val Debug = UnrealTargetConfiguration("Debug")
        val DebugGame =
            UnrealTargetConfiguration(
                "DebugGame",
            ) // DebugGame configuration; equivalent to development, but with optimization disabled for game modules
        val Development = UnrealTargetConfiguration("Development")
        val Test = UnrealTargetConfiguration("Test")
        val Shipping = UnrealTargetConfiguration("Shipping")

        val knownConfigurations =
            setOf(
                Debug,
                DebugGame,
                Development,
                Test,
                Shipping,
            )
    }
}
