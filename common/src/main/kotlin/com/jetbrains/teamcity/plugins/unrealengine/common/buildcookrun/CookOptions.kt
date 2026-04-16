package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

@JvmInline
value class CookCulture(
    val value: String,
)

@JvmInline
value class CookMap(
    val value: String,
)

data class CookOptions(
    val mapsToCook: List<CookMap>? = null,
    val cookCultures: List<CookCulture>? = null,
    val unversionedContent: Boolean = false,
    val generateChunks: Boolean = false,
    val iterativeCooking: Boolean = false,
    val cookAll: Boolean = false,
    val cookMapsOnly: Boolean = false,
    val cookPartialGC: Boolean = false,
    val fastCook: Boolean = false,
    val ignoreCookErrors: Boolean = false,
    val skipCookingEditorContent: Boolean = false,
    val excludeEditorContent: Boolean = false,
    val additionalCookerOptions: String? = null,
) {
    companion object {
        fun from(runnerParameters: Map<String, String>) =
            CookOptions(
                MapsToCookParameter.from(runnerParameters),
                CookCulturesParameter.from(runnerParameters),
                UnversionedCookedContentParameter.from(runnerParameters),
                runnerParameters[GenerateChunksParameter.name].toBoolean(),
                runnerParameters[IterativeCookingParameter.name].toBoolean(),
                runnerParameters[CookAllParameter.name].toBoolean(),
                runnerParameters[CookMapsOnlyParameter.name].toBoolean(),
                runnerParameters[CookPartialGCParameter.name].toBoolean(),
                runnerParameters[FastCookParameter.name].toBoolean(),
                runnerParameters[IgnoreCookErrorsParameter.name].toBoolean(),
                runnerParameters[SkipCookingEditorContentParameter.name].toBoolean(),
                runnerParameters[ExcludeEditorContentParameter.name].toBoolean(),
                AdditionalCookerOptionsParameter.from(runnerParameters),
            )
    }

    val arguments =
        buildList {
            add("-cook")

            if (mapsToCook != null) {
                add("-map=${mapsToCook.joinToString(separator = "+") { it.value }}")
            }

            if (cookCultures != null) {
                add("-cookcultures=${cookCultures.joinToString(separator = "+") { it.value }}")
            }

            if (unversionedContent) {
                add("-unversionedcookedcontent")
            }

            if (generateChunks) {
                add("-generatechunks")
            }

            if (iterativeCooking) {
                add("-iterativecooking")
            }

            if (cookAll) {
                add("-cookall")
            }

            if (cookMapsOnly) {
                add("-cookmapsonly")
            }

            if (cookPartialGC) {
                add("-CookPartialGC")
            }

            if (fastCook) {
                add("-FastCook")
            }

            if (ignoreCookErrors) {
                add("-IgnoreCookErrors")
            }

            if (skipCookingEditorContent) {
                add("-SkipCookingEditorContent")
            }

            if (excludeEditorContent) {
                add("-ExcludeEditorContent")
            }

            additionalCookerOptions?.let {
                add(AdditionalCookerOptionsParameter.toArgument(it))
            }
        }
}
