package buildgraph

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import com.jetbrains.teamcity.plugins.unrealengine.common.buildgraph.BuildGraphSettings
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildState
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildState.*
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateChanged
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateChanged.*
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.BadgeState
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsMetadataServerClient
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.Badge
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BadgePostingConfig
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphBadgePublisher
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.BuildGraphBuildSettings
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.getBuildGraphBuildSettings
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.getPerforceChangelistNumber
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildRevision
import jetbrains.buildServer.serverSide.BuildsManager
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.swarm.SwarmClientManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BuildGraphBadgePublisherTests {
    private val parentBuildId = 0xC00L
    private val buildGraphSettings = mockk<BuildGraphSettings>()
    private val buildsManager = mockk<BuildsManager>()
    private val links = mockk<RelativeWebLinks>()
    private val ugsMetadataServerClient = mockk<UgsMetadataServerClient>()
    private val build = mockk<SBuild>()

    private val stateAfterUpdate =
        DistributedBuildState(
            listOf(
                Build(
                    "1",
                    listOf(
                        BuildStep("1.1", BuildStepState.Completed, StepOutcome.Success),
                        BuildStep("1.2", BuildStepState.Interrupted),
                        BuildStep("1.3", BuildStepState.Skipped),
                        BuildStep("1.4", BuildStepState.Skipped),
                    ),
                ),
                Build(
                    "2",
                    listOf(
                        BuildStep("2.1", BuildStepState.Completed, StepOutcome.Success),
                        BuildStep("2.2", BuildStepState.Running),
                    ),
                ),
                Build(
                    "3",
                    listOf(
                        BuildStep("3.1", BuildStepState.Completed, StepOutcome.Success),
                        BuildStep("3.2", BuildStepState.Pending),
                    ),
                ),
                Build(
                    "4",
                    listOf(
                        BuildStep("4.1", BuildStepState.Completed, StepOutcome.Success),
                        BuildStep("4.1", BuildStepState.Completed, StepOutcome.Failure),
                    ),
                ),
                Build(
                    "5",
                    listOf(
                        BuildStep("5.1", BuildStepState.Running),
                    ),
                ),
                Build(
                    "6",
                    listOf(
                        BuildStep("6.1", BuildStepState.Completed, StepOutcome.Success),
                        BuildStep("6.2", BuildStepState.Completed, StepOutcome.Success),
                        BuildStep("6.3", BuildStepState.Completed, StepOutcome.Failure),
                    ),
                ),
                Build(
                    "7",
                    listOf(
                        BuildStep("7.1", BuildStepState.Skipped),
                        BuildStep("7.2", BuildStepState.Skipped),
                    ),
                ),
            ),
        )

    init {
        mockkStatic(SBuild::getBuildGraphBuildSettings)
        mockkStatic(BuildRevision::getPerforceChangelistNumber)
    }

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { buildGraphSettings.buildGraphGeneratedMarker } returns "marker"

        with(build) {
            every { buildPromotion } returns
                mockk<BuildPromotionEx> {
                    every { getAttribute("marker") } returns true.toString()
                }
            every { revisions } returns
                listOf(
                    mockk {
                        every { root } returns
                            mockk {
                                every { vcsName } returns SwarmClientManager.PERFORCE_VCS_NAME
                            }
                        every { getPerforceChangelistNumber() } returns 100L
                    },
                )
        }

        every {
            buildsManager.findBuildInstanceById(any())
        } returns build

        every { links.getBuildDependenciesUrl(build) } returns "url"

        coEvery {
            with(any<Raise<Error>>()) {
                ugsMetadataServerClient.postBuildMetadata(any(), any())
            }
        } just Runs
    }

    private fun `notifies about started badge cases`() =
        listOf(
            BadgeUpdateTestCase(
                event = BuildStepStarted(parentBuildId, stateAfterUpdate, "2.2"),
                badges = listOf(Badge("badge", "project", listOf("2.2"))),
                expectedUpdatedBadges = listOf("badge"),
            ),
            BadgeUpdateTestCase(
                event = BuildStepStarted(parentBuildId, stateAfterUpdate, "2.2"),
                badges =
                    listOf(
                        Badge("badge 1", "project", listOf("2.2")),
                        Badge("badge 2", "project", listOf("2.2")),
                    ),
                expectedUpdatedBadges = listOf("badge 1", "badge 2"),
            ),
            BadgeUpdateTestCase(
                event = BuildStepStarted(parentBuildId, stateAfterUpdate, "2.2"),
                badges = listOf(Badge("badge", "project", listOf("2.2", "3.2"))),
                expectedUpdatedBadges = listOf("badge"),
            ),
        )

    @ParameterizedTest
    @MethodSource("notifies about started badge cases")
    fun `notifies about started badge`(case: BadgeUpdateTestCase) =
        runTest {
            // arrange
            val config = build.enableBadgePosting(case.badges)

            // act
            act(case.event)

            // assert
            case.expectedUpdatedBadges.forEach { badge ->
                coVerify {
                    with(any<Raise<Error>>()) {
                        ugsMetadataServerClient.postBuildMetadata(
                            config.ugsMetadataServerUrl,
                            match {
                                it.badgeName == badge && it.badgeState == BadgeState.Starting
                            },
                        )
                    }
                }
            }
            confirmVerified(ugsMetadataServerClient)
        }

    private fun `does not notify about already started badge cases`() =
        listOf(
            BadgeUpdateTestCase(
                event = BuildStepStarted(parentBuildId, stateAfterUpdate, "2.2"),
                badges = listOf(Badge("badge", "project", listOf("2.2", "5.1"))),
                expectedUpdatedBadges = emptyList(),
            ),
            BadgeUpdateTestCase(
                event = BuildStepStarted(parentBuildId, stateAfterUpdate, "2.2"),
                badges = listOf(Badge("badge", "project", listOf("2.1", "2.2"))),
                expectedUpdatedBadges = emptyList(),
            ),
        )

    @ParameterizedTest
    @MethodSource("does not notify about already started badge cases")
    fun `does not notify about already started badge`(case: BadgeUpdateTestCase) =
        runTest {
            // arrange
            build.enableBadgePosting(case.badges)

            // act
            act(case.event)

            // assert
            verify { ugsMetadataServerClient wasNot Called }
        }

    private fun `notifies about succeeded badge cases`() =
        listOf(
            BadgeUpdateTestCase(
                event = BuildStepCompleted(parentBuildId, stateAfterUpdate, "1.1", StepOutcome.Success),
                badges = listOf(Badge("badge", "project", listOf("1.1"))),
                expectedUpdatedBadges = listOf("badge"),
            ),
            BadgeUpdateTestCase(
                event = BuildStepCompleted(parentBuildId, stateAfterUpdate, "1.1", StepOutcome.Success),
                badges = listOf(Badge("badge", "project", listOf("1.1", "2.1"))),
                expectedUpdatedBadges = listOf("badge"),
            ),
        )

    @ParameterizedTest
    @MethodSource("notifies about succeeded badge cases")
    fun `notifies about succeeded badge`(case: BadgeUpdateTestCase) =
        runTest {
            // arrange
            val config = build.enableBadgePosting(case.badges)

            // act
            act(case.event)

            // assert
            case.expectedUpdatedBadges.forEach { badge ->
                coVerify {
                    with(any<Raise<Error>>()) {
                        ugsMetadataServerClient.postBuildMetadata(
                            config.ugsMetadataServerUrl,
                            match {
                                it.badgeName == badge && it.badgeState == BadgeState.Success
                            },
                        )
                    }
                }
            }
            confirmVerified(ugsMetadataServerClient)
        }

    private fun `notifies about failed badge cases`() =
        listOf(
            BadgeUpdateTestCase(
                event = BuildStepCompleted(parentBuildId, stateAfterUpdate, "4.1", StepOutcome.Failure),
                badges = listOf(Badge("badge", "project", listOf("4.1"))),
                expectedUpdatedBadges = listOf("badge"),
            ),
            BadgeUpdateTestCase(
                event = BuildStepCompleted(parentBuildId, stateAfterUpdate, "4.1", StepOutcome.Failure),
                badges = listOf(Badge("badge", "project", listOf("3.2", "4.1", "5.1"))),
                expectedUpdatedBadges = listOf("badge"),
            ),
        )

    @ParameterizedTest
    @MethodSource("notifies about failed badge cases")
    fun `notifies about failed badge`(case: BadgeUpdateTestCase) =
        runTest {
            // arrange
            val config = build.enableBadgePosting(case.badges)

            // act
            act(case.event)

            // assert
            case.expectedUpdatedBadges.forEach { badge ->
                coVerify {
                    with(any<Raise<Error>>()) {
                        ugsMetadataServerClient.postBuildMetadata(
                            config.ugsMetadataServerUrl,
                            match {
                                it.badgeName == badge && it.badgeState == BadgeState.Failure
                            },
                        )
                    }
                }
            }
            confirmVerified(ugsMetadataServerClient)
        }

    private fun `does not notify about already failed badge cases`() =
        listOf(
            BadgeUpdateTestCase(
                event = BuildStepCompleted(parentBuildId, stateAfterUpdate, "6.3", StepOutcome.Failure),
                badges = listOf(Badge("badge", "project", listOf("6.3", "4.1"))),
                expectedUpdatedBadges = emptyList(),
            ),
            BadgeUpdateTestCase(
                event = BuildStepStarted(parentBuildId, stateAfterUpdate, "5.1"),
                badges = listOf(Badge("badge", "project", listOf("6.3", "5.1"))),
                expectedUpdatedBadges = emptyList(),
            ),
            BadgeUpdateTestCase(
                event = BuildStepCompleted(parentBuildId, stateAfterUpdate, "1.1", StepOutcome.Success),
                badges = listOf(Badge("badge", "project", listOf("6.3", "1.1"))),
                expectedUpdatedBadges = emptyList(),
            ),
        )

    @ParameterizedTest
    @MethodSource("does not notify about already failed badge cases")
    fun `does not notify about already failed badge`(case: BadgeUpdateTestCase) =
        runTest {
            // arrange
            build.enableBadgePosting(case.badges)

            // act
            act(case.event)

            // assert
            verify { ugsMetadataServerClient wasNot Called }
        }

    private fun `notifies about skipped badge cases`() =
        listOf(
            BadgeUpdateTestCase(
                event = BuildStepInterrupted(parentBuildId, stateAfterUpdate, "1", "1.2"),
                badges =
                    listOf(
                        Badge("badge 1", "project", listOf("1.2")),
                        Badge("badge 2", "project", listOf("1.1", "1.2")),
                        Badge("badge 3", "project", listOf("2.1", "1.2")),
                    ),
                expectedUpdatedBadges = listOf("badge 1", "badge 2", "badge 3"),
            ),
            BadgeUpdateTestCase(
                event = BuildStepCompleted(parentBuildId, stateAfterUpdate, "3.1", StepOutcome.Success),
                badges = listOf(Badge("badge", "project", listOf("3.1", "1.4"))),
                expectedUpdatedBadges = listOf("badge"),
            ),
        )

    @ParameterizedTest
    @MethodSource("notifies about skipped badge cases")
    fun `notifies about skipped badge`(case: BadgeUpdateTestCase) =
        runTest {
            // arrange
            val config = build.enableBadgePosting(case.badges)

            // act
            act(case.event)

            // assert
            case.expectedUpdatedBadges.forEach { badge ->
                coVerify {
                    with(any<Raise<Error>>()) {
                        ugsMetadataServerClient.postBuildMetadata(
                            config.ugsMetadataServerUrl,
                            match {
                                it.badgeName == badge && it.badgeState == BadgeState.Skipped
                            },
                        )
                    }
                }
            }
            confirmVerified(ugsMetadataServerClient)
        }

    private fun `does not notify about skipped badge cases`() =
        listOf(
            BadgeUpdateTestCase(
                event = BuildStepInterrupted(parentBuildId, stateAfterUpdate, "1", "1.2"),
                badges = listOf(Badge("badge", "project", listOf("5.1", "1.2"))),
                expectedUpdatedBadges = emptyList(),
            ),
            BadgeUpdateTestCase(
                event = BuildSkipped(parentBuildId, stateAfterUpdate, "7"),
                badges = listOf(Badge("badge", "project", listOf("7.1", "7.2", "2.2"))),
                expectedUpdatedBadges = emptyList(),
            ),
            BadgeUpdateTestCase(
                event = BuildStepCompleted(parentBuildId, stateAfterUpdate, "3.1", StepOutcome.Success),
                badges = listOf(Badge("badge", "project", listOf("3.1", "7.2", "5.1"))),
                expectedUpdatedBadges = emptyList(),
            ),
        )

    @ParameterizedTest
    @MethodSource("does not notify about skipped badge cases")
    fun `does not notify about skipped badge`(case: BadgeUpdateTestCase) =
        runTest {
            // arrange
            build.enableBadgePosting(case.badges)

            // act
            act(case.event)

            // assert
            verify { ugsMetadataServerClient wasNot Called }
        }

    data class BadgeUpdateTestCase(
        val event: DistributedBuildStateChanged,
        val badges: Collection<Badge>,
        val expectedUpdatedBadges: Collection<String>,
    )

    private suspend fun act(event: DistributedBuildStateChanged) {
        createInstance().consume(event)
    }

    private fun SBuild.enableBadgePosting(badges: Collection<Badge>): BadgePostingConfig.Enabled {
        val badgePostingConfig = BadgePostingConfig.Enabled(UgsMetadataServerUrl("url"), badges)
        every {
            with(any<Raise<Error>>()) {
                getBuildGraphBuildSettings()
            }
        } returns BuildGraphBuildSettings(badgePostingConfig)
        return badgePostingConfig
    }

    private fun createInstance() =
        BuildGraphBadgePublisher(
            buildGraphSettings,
            buildsManager,
            links,
            ugsMetadataServerClient,
        )
}
