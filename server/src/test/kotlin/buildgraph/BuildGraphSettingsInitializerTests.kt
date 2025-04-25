package buildgraph

import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphModeParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.PostBadgesFromGraphParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.UgsMetadataServerUrlParameter
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.Badge
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BadgePostingConfig
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphSettingsInitializer
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.addBuildGraphBuildSettings
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.activeRunners
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import jetbrains.buildServer.serverSide.BuildPromotion
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BuildGraphSettingsInitializerTests {
    private val initializer = BuildGraphSettingsInitializer()

    init {
        mockkStatic(BuildPromotion::activeRunners)
        mockkStatic(SBuild::addBuildGraphBuildSettings)
    }

    data class TestCase(
        val build: SBuild,
        val badges: Collection<Badge>,
        val expectedConfig: BadgePostingConfig?,
        val expectedError: GenericError? = null,
    )

    private fun `happy path cases`(): List<TestCase> {
        val badges =
            listOf(
                Badge("Badge1", "Project1", listOf("Node1")),
                Badge("Badge2", "Project2", listOf("Node2")),
            )

        val runnerWithMetadata =
            mockk<SBuildRunnerDescriptor> {
                every { parameters } returns
                    mapOf(
                        BuildGraphModeParameter.name to "Distributed",
                        PostBadgesFromGraphParameter.name to true.toString(),
                        UgsMetadataServerUrlParameter.name to "http://metadata-server",
                    )
            }

        val buildWithMetadata =
            mockk<SBuild> {
                every { addBuildGraphBuildSettings(any()) } just Runs
                every { buildPromotion } returns
                    mockk<BuildPromotion> {
                        every { activeRunners() } returns listOf(runnerWithMetadata)
                    }
            }

        val runnerWithoutMetadata =
            mockk<SBuildRunnerDescriptor> {
                every { parameters } returns
                    mapOf(
                        BuildGraphModeParameter.name to "Distributed",
                    )
            }

        val buildWithoutMetadata =
            mockk<SBuild> {
                every { addBuildGraphBuildSettings(any()) } just Runs
                every { buildPromotion } returns
                    mockk<BuildPromotion> {
                        every { activeRunners() } returns listOf(runnerWithoutMetadata)
                    }
            }

        return listOf(
            TestCase(
                build = buildWithMetadata,
                badges = badges,
                expectedConfig = BadgePostingConfig.Enabled(UgsMetadataServerUrl("http://metadata-server"), badges),
            ),
            TestCase(
                build = buildWithoutMetadata,
                badges = badges,
                expectedConfig = BadgePostingConfig.Disabled,
            ),
        )
    }

    private fun `error cases`(): List<TestCase> {
        val badges =
            listOf(
                Badge("Badge1", "Project1", listOf("Node1")),
                Badge("Badge2", "Project2", listOf("Node2")),
            )

        val buildWithNoRunners =
            mockk<SBuild> {
                every { buildPromotion } returns
                    mockk<BuildPromotion> {
                        every { activeRunners() } returns emptyList()
                    }
            }

        val buildWithMultipleRunners =
            mockk<SBuild> {
                every { buildPromotion } returns
                    mockk<BuildPromotion> {
                        every { activeRunners() } returns
                            listOf(
                                mockk<SBuildRunnerDescriptor>(),
                                mockk<SBuildRunnerDescriptor>(),
                            )
                    }
            }

        return listOf(
            TestCase(
                build = buildWithNoRunners,
                badges = badges,
                expectedConfig = null,
                expectedError = GenericError("Unable to get runner parameters (there should be exactly one active UnrealEngine runner)"),
            ),
            TestCase(
                build = buildWithMultipleRunners,
                badges = badges,
                expectedConfig = null,
                expectedError = GenericError("Unable to get runner parameters (there should be exactly one active UnrealEngine runner)"),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("happy path cases")
    fun `initializes build settings with correct badge posting config`(case: TestCase) {
        // act
        val result = either { initializer.initializeBuildSettings(case.build, case.badges) }

        // assert
        result.isRight() shouldBe true
        result.getOrNull()?.badgePosting shouldBe case.expectedConfig
    }

    @ParameterizedTest
    @MethodSource("error cases")
    fun `fails to initialize build settings when runner configuration is invalid`(case: TestCase) {
        // act
        val result = either { initializer.initializeBuildSettings(case.build, case.badges) }

        // assert
        result.isLeft() shouldBe true
        result.leftOrNull() shouldBe case.expectedError
    }
}
