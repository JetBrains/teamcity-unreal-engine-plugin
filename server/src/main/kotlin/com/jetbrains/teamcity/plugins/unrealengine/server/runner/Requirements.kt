package com.jetbrains.teamcity.plugins.unrealengine.server.runner

import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineIdentifier
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRunner
import jetbrains.buildServer.requirements.Requirement
import jetbrains.buildServer.requirements.RequirementQualifier
import jetbrains.buildServer.requirements.RequirementType

object Requirements {
    fun engineExists(version: UnrealEngineIdentifier): Requirement {
        val path = UnrealEngineRunner.AGENT_PARAMETER_NAME_PREFIX + "." + escape(version.value) + ".*"
        return Requirement(RequirementQualifier.EXISTS_QUALIFIER + path, null, RequirementType.EXISTS)
    }

    private fun escape(value: String): String = if (value.contains('%')) value else value.replace(".", "\\.")
}
