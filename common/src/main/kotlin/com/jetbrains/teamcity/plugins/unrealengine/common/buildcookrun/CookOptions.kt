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
        // TODO: move to parameter
        fun from(runnerParameters: Map<String, String>): CookOptions {
            val maps =
                runnerParameters[MapsToCookParameter.name]
                    ?.splitToSequence(SEPARATOR)
                    ?.map { CookMap(it) }
                    ?.toList()

            val cultures =
                runnerParameters[CookCulturesParameter.name]
                    ?.splitToSequence(SEPARATOR)
                    ?.map { CookCulture(it) }
                    ?.toList()

            return CookOptions(maps, cultures, runnerParameters[UnversionedCookedContentParameter.name].toBoolean())
        }

        private const val SEPARATOR = "+"
    }

    val arguments =
        buildList {
            add("-cook")

            if (mapsToCook != null) {
                add("-map=${mapsToCook.joinToString(separator = SEPARATOR) { it.value }}")
            }

            if (cookCultures != null) {
                add("-cookcultures=${cookCultures.joinToString(separator = SEPARATOR) { it.value }}")
            }

            if (unversionedContent) {
                add("-unversionedcookedcontent")
            }
        }
}
