package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildTypeSettings
import jetbrains.buildServer.serverSide.SimpleParameter
import jetbrains.buildServer.virtualConfiguration.generator.VirtualBuildTypeSettings
import jetbrains.buildServer.virtualConfiguration.generator.VirtualPromotionGeneratorFactory

class BuildGraphVirtualBuildCreator(
    private val buildGeneratorFactory: VirtualPromotionGeneratorFactory,
) {
    private val restrictedIdCharactersRegex = "[^A-Za-z0-9_]".toRegex()

    interface VirtualBuildCreationContext {
        val originalBuild: BuildPromotionEx
    }

    fun inContextOf(originalBuild: BuildPromotionEx): VirtualBuildCreationContext =
        object : VirtualBuildCreationContext {
            override val originalBuild = originalBuild
        }

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
                originalBuild.buildParameters.map { SimpleParameter(it.key, it.value) },
            )

        val build =
            buildCreator.getOrCreate(virtualBuildTypeSettings) { buildConfiguration, _ ->
                configureSettings(buildConfiguration)
                val changed = true
                changed
            } as BuildPromotionEx

        build.setRevisionsFrom(originalBuild)

        return build
    }

    private fun String.toExternalId() = restrictedIdCharactersRegex.replace(this, "_")
}
