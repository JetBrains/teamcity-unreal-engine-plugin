package com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asBuildPromotionEx
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.hasSingleDistributedBuildGraphStep
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SRunningBuild

data class ValidatedSetupBuild(
    val build: SRunningBuild,
) : SRunningBuild by build

data class ValidationResult(
    val setupBuild: ValidatedSetupBuild,
    val originalBuildGraphBuild: SBuild,
)

@JvmInline
value class BuildSkipped(
    val message: String,
) : Error

class BuildGraphSetupBuildValidator(
    private val settings: BuildGraphSettings,
) {
    context(Raise<Error>)
    fun validate(setupBuild: SRunningBuild): ValidationResult {
        val buildType =
            ensureNotNull(setupBuild.buildType) {
                BuildSkipped("The running build \"${setupBuild.fullName}\" is missing a build type")
            }

        val isBuildGraphSetup =
            buildType.isVirtual &&
                setupBuild.buildPromotion.hasSingleDistributedBuildGraphStep() &&
                buildType.name == settings.setupBuildName

        ensure(isBuildGraphSetup) {
            BuildSkipped("The running build \"${setupBuild.fullName}\" isn't a build graph setup build")
        }

        ensureNotNull(setupBuild.projectId) {
            BuildSkipped("Build graph setup build is missing project id. Distributed build won't be created")
        }

        val originalBuild =
            setupBuild.buildPromotion
                .asBuildPromotionEx()
                .let {
                    ensureNotNull(
                        it.dependedOnMe
                            .singleOrNull()
                            ?.dependent
                            ?.associatedBuild,
                        "Unable to find the original build for ${setupBuild.fullName}",
                    )
                }

        return ValidationResult(ValidatedSetupBuild(setupBuild), originalBuild)
    }
}
