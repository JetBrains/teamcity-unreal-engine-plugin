package com.jetbrains.teamcity.plugins.unrealengine.common.parameters

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.raise
import com.jetbrains.teamcity.plugins.unrealengine.common.EngineDetectionMode
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineIdentifier
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealEngineRootPath

object EngineDetectionModeParameter : SelectParameter() {
    @Suppress("MayBeConstant", "RedundantSuppression") // constants aren't accessible from JSP
    val automatic = SelectOption("automatic-detection-mode", "Automatic")

    @Suppress("MayBeConstant", "RedundantSuppression") // constants aren't accessible from JSP
    val manual = SelectOption("manual-detection-mode", "Manual")

    override val name = "engine-detection-mode"
    override val displayName = "Engine detection mode"
    override val defaultValue = automatic.name
    override val description = null

    override val options = listOf(automatic, manual)

    context(_: Raise<PropertyValidationError>)
    fun parseDetectionMode(runnerParameters: Map<String, String>): EngineDetectionMode =
        when (runnerParameters[name]) {
            automatic.name -> {
                val engineIdentifier = runnerParameters[UnrealEngineIdentifierParameter.name]
                if (engineIdentifier.isNullOrEmpty()) {
                    raise(
                        PropertyValidationError(
                            UnrealEngineIdentifierParameter.name,
                            "The engine version cannot be empty.",
                        ),
                    )
                }
                EngineDetectionMode.Automatic(UnrealEngineIdentifier(engineIdentifier))
            }
            manual.name -> {
                val engineRootDirectory = runnerParameters[UnrealEngineRootParameter.name]
                if (engineRootDirectory.isNullOrEmpty()) {
                    raise(
                        PropertyValidationError(
                            UnrealEngineRootParameter.name,
                            "The engine root path must be set.",
                        ),
                    )
                }
                EngineDetectionMode.Manual(UnrealEngineRootPath(engineRootDirectory))
            }
            else -> {
                raise(PropertyValidationError(name, "Unknown detection mode."))
            }
        }
}

object UnrealEngineIdentifierParameter : TextInputParameter {
    override val name = "engine-identifier"
    override val displayName = "Identifier"
    override val defaultValue = ""
    override val description =
        "Choose a specific version of Unreal Engine that should be used. " +
            "You can enter regular numeric identifiers (for example, \"5.2\" or \"5.3.4\") for default versions " +
            "installed on agent machines, or custom identifiers for source-built versions."
    override val required = true
    override val supportsVcsNavigation = false
    override val expandable = false
    override val advanced = false
}

object UnrealEngineRootParameter : TextInputParameter {
    override val name = "engine-root-path"
    override val displayName = "Root dir"
    override val defaultValue = ""
    override val description = "The path (relative to the checkout directory or absolute) to the Unreal Engine root folder."
    override val required = true
    override val supportsVcsNavigation = true
    override val expandable = false
    override val advanced = false
}
