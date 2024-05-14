package com.jetbrains.teamcity.plugins.unrealengine.agent

import com.jetbrains.teamcity.plugins.framework.agent.PrimaryAgentParametersSupplier
import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.agent.config.AgentParametersSupplier

class ExtensionRegistration(
    primaryAgentParametersSupplier: PrimaryAgentParametersSupplier,
    extensionHolder: ExtensionHolder,
) {
    init {
        extensionHolder.registerExtension(
            AgentParametersSupplier::class.java,
            AgentParametersSupplier::class.java.name,
            primaryAgentParametersSupplier,
        )
    }
}
