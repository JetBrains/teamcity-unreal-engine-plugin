package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookCulturesParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.MapsToCookParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnversionedCookedContentParameter

class CookComponent {
    val cook = CookStageSwitchParameter
    val maps = MapsToCookParameter
    val cultures = CookCulturesParameter
    val unversionedContent = UnversionedCookedContentParameter

    fun formatFlags(properties: Map<String, String>) = ComponentParametersFormatter.formatFlags(
        sequenceOf(
            unversionedContent,
        ),
        properties,
    )
}
