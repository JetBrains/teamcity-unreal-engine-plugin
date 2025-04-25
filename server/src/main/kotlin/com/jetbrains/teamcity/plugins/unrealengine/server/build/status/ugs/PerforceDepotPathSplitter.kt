package com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs

@JvmInline
value class PerforceDepotPath(
    val value: String,
)

@JvmInline
value class PerforceStream(
    val value: String,
)

@JvmInline
value class PerforceFilePath(
    val value: String,
)

/**
 * Splits a Perforce depot path into two parts:
 * - The stream path (first 4 segments, e.g. `//depot/stream`)
 * - The remaining file path inside the stream
 *
 * This logic follows the conventions used in Horde and UGS,
 * where Perforce depot paths are assumed to follow the structure:
 * `//depot/stream/project/...`
 *
 * For example:
 *   Input: "//depot/main/project/module"
 *   Output: ("//depot/main", "project/module")
 */
class PerforceDepotPathSplitter {
    fun split(depotPath: PerforceDepotPath): Pair<PerforceStream, PerforceFilePath> {
        val parts =
            depotPath.value
                .lowercase()
                .trimEnd('/')
                .split("/")
        val stream = PerforceStream(parts.take(4).joinToString("/"))
        val filePath = PerforceFilePath(parts.drop(4).joinToString("/"))
        return stream to filePath
    }
}
