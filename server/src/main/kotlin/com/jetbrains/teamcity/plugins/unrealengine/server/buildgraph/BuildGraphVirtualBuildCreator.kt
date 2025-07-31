package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asBuildPromotionEx
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.create
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.generateIdForVirtualBuild
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.markAsGeneratedBy
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.setRevisionsFrom
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

    context(context: VirtualBuildCreationContext)
    fun create(
        name: String,
        configureSettings: BuildTypeSettings.() -> Unit,
    ): BuildPromotionEx {
        val buildCreator = buildGeneratorFactory.create(context.originalBuild)

        val virtualBuildTypeSettings =
            VirtualBuildTypeSettings(
                context.originalBuild.generateIdForVirtualBuild(name).toExternalId(),
                name,
            ).setParameters(
                context.originalBuild.parameters.map { SimpleParameter(it.key, it.value) },
            )

        val build =
            buildCreator
                .getOrCreate(virtualBuildTypeSettings) { buildConfiguration, _ ->
                    buildConfiguration.checkoutDirectory = context.originalBuild.checkoutDirectory
                    buildConfiguration.checkoutType = context.originalBuild.buildSettings.checkoutType
                    configureSettings(buildConfiguration)
                    val changed = true
                    changed
                }.asBuildPromotionEx()

        build.setRevisionsFrom(context.originalBuild)
        build.markAsGeneratedBy(context.originalBuild)

        return build
    }

    private fun String.toExternalId() = restrictedIdCharactersRegex.replace(this, "_")
}
