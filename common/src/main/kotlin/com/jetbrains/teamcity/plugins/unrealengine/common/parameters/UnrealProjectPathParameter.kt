package com.jetbrains.teamcity.plugins.unrealengine.common.parameters

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.raise
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealProjectPath

// identical parameters should have different names in different contexts (within build-cook-run or run-automation-tests),
// since they are all on the same page at the same time
class UnrealProjectPathParameter(
    override val name: String,
) : TextInputParameter {
    override val displayName = "Project"
    override val description = null
    override val defaultValue = ""
    override val required = true
    override val supportsVcsNavigation = true
    override val expandable = false
    override val advanced = false

    context(_: Raise<PropertyValidationError>)
    fun parseProjectPath(runnerParameters: Map<String, String>): UnrealProjectPath {
        val projectPath = runnerParameters[name]
        if (projectPath.isNullOrEmpty()) {
            raise(PropertyValidationError(name, "Project path must not be empty"))
        }

        return UnrealProjectPath(projectPath)
    }
}
