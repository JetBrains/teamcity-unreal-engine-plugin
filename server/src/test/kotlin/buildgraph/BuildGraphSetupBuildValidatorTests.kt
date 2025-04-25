package buildgraph

import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphSettings
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphSetupBuildValidator
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildSkipped
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.asBuildPromotionEx
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.hasSingleDistributedBuildGraphStep
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.SRunningBuild
import jetbrains.buildServer.serverSide.dependency.BuildDependencyEx
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BuildGraphSetupBuildValidatorTests {
    private val settings = BuildGraphSettings()
    private val validator = BuildGraphSetupBuildValidator(settings)

    init {
        mockkStatic(BuildPromotion::hasSingleDistributedBuildGraphStep)
        mockkStatic(BuildPromotion::asBuildPromotionEx)
    }

    data class TestCase(
        val setupBuild: SRunningBuild,
        val expectedError: Error? = null,
    )

    private fun `happy path cases`(): List<TestCase> {
        val validBuildType =
            mockk<SBuildType> {
                every { isVirtual } returns true
                every { name } returns settings.setupBuildName
            }

        val originalBuild = mockk<SBuild>()

        val validPromotion =
            mockk<BuildPromotion> {
                every { hasSingleDistributedBuildGraphStep() } returns true
            }

        val validPromotionEx =
            mockk<BuildPromotionEx> {
                every { dependedOnMe } returns
                    listOf(
                        mockk<BuildDependencyEx> {
                            every { dependent } returns
                                mockk {
                                    every { associatedBuild } returns originalBuild
                                }
                        },
                    )
            }

        every { validPromotion.asBuildPromotionEx() } returns validPromotionEx

        val validBuild =
            mockk<SRunningBuild> {
                every { buildType } returns validBuildType
                every { buildPromotion } returns validPromotion
                every { projectId } returns "project-1"
                every { fullName } returns "Build #1"
            }

        return listOf(
            TestCase(
                setupBuild = validBuild,
            ),
        )
    }

    private fun `error cases`(): List<TestCase> {
        val validBuildType =
            mockk<SBuildType> {
                every { isVirtual } returns true
                every { name } returns settings.setupBuildName
            }

        val validPromotion =
            mockk<BuildPromotion> {
                every { hasSingleDistributedBuildGraphStep() } returns true
            }

        val invalidBuildType =
            mockk<SBuildType> {
                every { isVirtual } returns false
                every { name } returns "Invalid"
            }

        val invalidBuild =
            mockk<SRunningBuild> {
                every { buildType } returns invalidBuildType
                every { buildPromotion } returns validPromotion
                every { projectId } returns "project-1"
                every { fullName } returns "Build #2"
            }

        val missingProjectIdBuild =
            mockk<SRunningBuild> {
                every { buildType } returns validBuildType
                every { buildPromotion } returns validPromotion
                every { projectId } returns null
                every { fullName } returns "Build #3"
            }

        val promotionWithNoDependencies =
            mockk<BuildPromotion> {
                every { hasSingleDistributedBuildGraphStep() } returns true
                every { asBuildPromotionEx() } returns
                    mockk<BuildPromotionEx> {
                        every { dependedOnMe } returns emptyList()
                    }
            }

        val buildWithNoDependencies =
            mockk<SRunningBuild> {
                every { buildType } returns validBuildType
                every { buildPromotion } returns promotionWithNoDependencies
                every { projectId } returns "project-1"
                every { fullName } returns "Build #4"
            }

        val promotionWithMultipleDependencies =
            mockk<BuildPromotion> {
                every { hasSingleDistributedBuildGraphStep() } returns true
                every { asBuildPromotionEx() } returns
                    mockk<BuildPromotionEx> {
                        every { dependedOnMe } returns
                            listOf(
                                mockk<BuildDependencyEx>(),
                                mockk<BuildDependencyEx>(),
                            )
                    }
            }

        val buildWithMultipleDependencies =
            mockk<SRunningBuild> {
                every { buildType } returns validBuildType
                every { buildPromotion } returns promotionWithMultipleDependencies
                every { projectId } returns "project-1"
                every { fullName } returns "Build #5"
            }

        val promotionWithMissingAssociatedBuild =
            mockk<BuildPromotion> {
                every { hasSingleDistributedBuildGraphStep() } returns true
                every { asBuildPromotionEx() } returns
                    mockk<BuildPromotionEx> {
                        every { dependedOnMe } returns
                            listOf(
                                mockk<BuildDependencyEx> {
                                    every { dependent } returns
                                        mockk {
                                            every { associatedBuild } returns null
                                        }
                                },
                            )
                    }
            }

        val buildWithMissingAssociatedBuild =
            mockk<SRunningBuild> {
                every { buildType } returns validBuildType
                every { buildPromotion } returns promotionWithMissingAssociatedBuild
                every { projectId } returns "project-1"
                every { fullName } returns "Build #6"
            }

        return listOf(
            TestCase(
                setupBuild = invalidBuild,
                expectedError = BuildSkipped("The running build \"Build #2\" isn't a build graph setup build"),
            ),
            TestCase(
                setupBuild = missingProjectIdBuild,
                expectedError = BuildSkipped("Build graph setup build is missing project id. Distributed build won't be created"),
            ),
            TestCase(
                setupBuild = buildWithNoDependencies,
                expectedError = GenericError("Unable to find the original build for Build #4", null),
            ),
            TestCase(
                setupBuild = buildWithMultipleDependencies,
                expectedError = GenericError("Unable to find the original build for Build #5", null),
            ),
            TestCase(
                setupBuild = buildWithMissingAssociatedBuild,
                expectedError = GenericError("Unable to find the original build for Build #6", null),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("happy path cases")
    fun `returns validated build when the build is valid`(case: TestCase) {
        // act
        val result = either { validator.validate(case.setupBuild) }

        // assert
        result.isRight() shouldBe true
        result.getOrNull()?.setupBuild?.build shouldBe case.setupBuild
    }

    @ParameterizedTest
    @MethodSource("error cases")
    fun `raises error when the build is invalid`(case: TestCase) {
        // act
        val result = either { validator.validate(case.setupBuild) }

        // assert
        result.isLeft() shouldBe true
        result.leftOrNull() shouldBe case.expectedError
    }
}
