package com.jetbrains.teamcity.plugins.unrealengine.server.build

import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.asBuildPromotionEx
import jetbrains.buildServer.serverSide.CustomDataStorage
import jetbrains.buildServer.serverSide.SBuild

// Currently, this is not used for anything other than marking a build as using custom data storage.
// The reason for this is that all data storage entries are tied to their corresponding builds in terms of lifetime,
// and they are cleared as soon as the builds are removed (either due to cleanup or other reasons).
// Each build uses only a small portion of the storage,
// so we don’t expect this to become an issue in the near future and don’t feel the need for ASAP cleanup.
// (after the build finishes, all notifications are sent, and we no longer require the data)
private const val DATA_STORAGE_MARKER = "teamcity.build.unreal-engine.uses-storage"
private const val STORAGE_DELIMITER = ";"

fun SBuild.getUnrealDataStorage(name: String): CustomDataStorage {
    val buildPromotion = buildPromotion.asBuildPromotionEx()
    val usedStorage = buildPromotion.getAttribute(DATA_STORAGE_MARKER) as? String ?: ""

    if (usedStorage.isEmpty()) {
        buildPromotion.setAttribute(DATA_STORAGE_MARKER, name)
    } else if (!usedStorage.split(STORAGE_DELIMITER).contains(name)) {
        buildPromotion.setAttribute(DATA_STORAGE_MARKER, "$usedStorage$STORAGE_DELIMITER$name")
    }

    return getCustomDataStorage("unreal-engine.storage.$name")
}
