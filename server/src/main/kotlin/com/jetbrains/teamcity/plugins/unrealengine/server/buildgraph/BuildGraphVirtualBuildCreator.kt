package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.SimpleParameter
import jetbrains.buildServer.virtualConfiguration.generator.VirtualBuildTypeSettings
import jetbrains.buildServer.virtualConfiguration.generator.VirtualPromotionGeneratorFactory

class BuildGraphVirtualBuildCreator(
    private val buildGeneratorFactory: VirtualPromotionGeneratorFactory,
) {
    private val restrictedIdCharactersRegex = "[^A-Za-z0-9_]".toRegex()

    data class VirtualBuildCreationContext(
        val originalBuild: BuildPromotionEx,
    )

    fun inContextOf(originalBuild: BuildPromotion): VirtualBuildCreationContext =
        VirtualBuildCreationContext(originalBuild.asBuildPromotionEx())

    context(VirtualBuildCreationContext)
    fun create(
        name: String,
        configureSettings: BuildTypeSettings.() -> Unit,
    ): BuildPromotionEx {
        val buildCreator = buildGeneratorFactory.create(originalBuild)

        val virtualBuildTypeSettings =
            VirtualBuildTypeSettings(
                originalBuild.generateIdForVirtualBuild(name).toExternalId(),
                name,
            ).setParameters(
                originalBuild.parameters.map { SimpleParameter(it.key, it.value) },
            )

        val build =
            buildCreator
                .getOrCreate(virtualBuildTypeSettings) { buildConfiguration, _ ->
                    buildConfiguration.checkoutDirectory = originalBuild.checkoutDirectory
                    buildConfiguration.checkoutType = originalBuild.buildSettings.checkoutType
                    configureSettings(buildConfiguration)
                    val changed = true
                    changed
                }.asBuildPromotionEx()

        build.setRevisionsFrom(originalBuild)
        build.markAsGeneratedBy(originalBuild)

        return build
    }

    private fun String.toExternalId() = restrictedIdCharactersRegex.replace(this, "_")
}
