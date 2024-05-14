package com.jetbrains.teamcity.plugins.unrealengine.common.buildcookrun

@JvmInline
value class CookCulture(val value: String)

@JvmInline
value class CookMap(val value: String)

data class CookOptions(
    val mapsToCook: List<CookMap>? = null,
    val cookCultures: List<CookCulture>? = null,
    val unversionedContent: Boolean = false,
) {
    companion object {
        // TODO: move to parameter
        fun from(runnerParameters: Map<String, String>): CookOptions {
            val maps = runnerParameters[MapsToCookParameter.name]
                ?.splitToSequence(separator)
                ?.map { CookMap(it) }
                ?.toList()

            val cultures = runnerParameters[CookCulturesParameter.name]
                ?.splitToSequence(separator)
                ?.map { CookCulture(it) }
                ?.toList()

            return CookOptions(maps, cultures, runnerParameters[UnversionedCookedContentParameter.name].toBoolean())
        }

        private const val separator = "+"
    }

    val arguments = buildList {
        add("-cook")

        if (mapsToCook != null) {
            add("-map=${mapsToCook.joinToString(separator = separator) { it.value }}")
        }

        if (cookCultures != null) {
            add("-cookcultures=${cookCultures.joinToString(separator = separator) { it.value }}")
        }

        if (unversionedContent) {
            add("-unversionedcookedcontent")
        }
    }
}
