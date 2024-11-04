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
) {
    companion object {
        fun from(runnerParameters: Map<String, String>) =
            CookOptions(
                MapsToCookParameter.from(runnerParameters),
                CookCulturesParameter.from(runnerParameters),
                UnversionedCookedContentParameter.from(runnerParameters),
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
        }
}
