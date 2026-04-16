package com.jetbrains.teamcity.plugins.unrealengine.server.runner.ui

import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookCulturesParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookAllParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookMapsOnlyParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookPartialGCParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.CookStageSwitchParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.ExcludeEditorContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.FastCookParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.GenerateChunksParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.IgnoreCookErrorsParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.IterativeCookingParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.MapsToCookParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.SkipCookingEditorContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.UnversionedCookedContentParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun.AdditionalCookerOptionsParameter

class CookComponent {
    val cook = CookStageSwitchParameter
    val maps = MapsToCookParameter
    val cultures = CookCulturesParameter
    val unversionedContent = UnversionedCookedContentParameter
    val generateChunks = GenerateChunksParameter
    val iterativeCooking = IterativeCookingParameter
    val cookAll = CookAllParameter
    val cookMapsOnly = CookMapsOnlyParameter
    val cookPartialGC = CookPartialGCParameter
    val fastCook = FastCookParameter
    val ignoreCookErrors = IgnoreCookErrorsParameter
    val skipEditorContent = SkipCookingEditorContentParameter
    val excludeEditorContent = ExcludeEditorContentParameter
    val additionalCookerOptions = AdditionalCookerOptionsParameter

    fun formatFlags(properties: Map<String, String>) =
        ComponentParametersFormatter.formatFlags(
            sequenceOf(
                unversionedContent,
                generateChunks,
                iterativeCooking,
                cookAll,
                cookMapsOnly,
                cookPartialGC,
                fastCook,
                ignoreCookErrors,
                skipEditorContent,
                excludeEditorContent,
            ),
            properties,
        )
}
