package build.state

import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.RunnerInternalParameters
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import com.jetbrains.teamcity.plugins.unrealengine.server.build.DistributedBuild
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildEvent
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildState
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildState.*
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateChanged
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateChangedEventBus
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateStorage
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateTracker
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.getGeneratedById
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.BuildPromotionManager
import jetbrains.buildServer.serverSide.BuildTypeSettingsEx
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SBuildStepDescriptor
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DistributedBuildStateTrackerTests {
    private val parentBuildId = 0xC00L
    private val promotionManager = mockk<BuildPromotionManager>()
    private val stateStorage = mockk<DistributedBuildStateStorage>()
    private val parentBuild = mockk<SBuild>()
    private val eventBus = mockk<DistributedBuildStateChangedEventBus>()

    init {
        mockkStatic(BuildPromotionEx::getGeneratedById)
    }

    @BeforeEach
    fun init() {
        clearMocks(promotionManager, stateStorage, parentBuild, eventBus)

        coEvery { eventBus.dispatch(any()) } just Runs

        with(parentBuild) {
            every { buildId } returns parentBuildId
        }

        with(stateStorage) {
            every { init(any(), any()) } just Runs
            every {
                with(any<Raise<Error>>()) {
                    update(any(), any())
                }
            } returns DistributedBuildState(listOf())
            every { dispose(any()) } just Runs
        }
    }

    @Nested
    @DisplayName("on initiating tracking")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnInitiatingTracking {
        // arrange
        private val buildTypeSettings = mockk<BuildTypeSettingsEx>()

        private val originalRunnerParameters =
            mapOf(
                "foo" to "bar",
                "foo 2" to "bar 2",
            )

        private val distributedBuild =
            DistributedBuild(
                listOf(
                    createPromotion(
                        "1",
                        buildTypeSettings,
                        listOf(
                            createRunner("1.1", originalRunnerParameters),
                        ),
                    ),
                    createPromotion(
                        "2",
                        buildTypeSettings,
                        listOf(
                            createRunner("2.1", originalRunnerParameters),
                        ),
                    ),
                ),
            )

        @BeforeEach
        fun setup() {
            // arrange
            clearMocks(buildTypeSettings)

            with(buildTypeSettings) {
                every { updateBuildRunner(any(), any(), any(), any()) } returns true
            }

            // act
            createInstance().track(parentBuild, distributedBuild)
        }

        @Test
        fun `asks each runner of every build in the distributed build to report its state`() {
            // assert
            sequenceOf("1.1", "2.1").forEach { runner ->
                verify(exactly = 1) {
                    buildTypeSettings.updateBuildRunner(
                        runner,
                        runner,
                        runner,
                        match { updatedMap ->
                            updatedMap[RunnerInternalParameters.BUILD_STEP_NOTIFICATIONS_ENABLED].toBoolean()
                        },
                    )
                }
            }
        }

        @Test
        fun `does not modify original runners' parameters`() {
            // assert
            sequenceOf("1.1", "2.1").forEach { runner ->
                verify(exactly = 1) {
                    buildTypeSettings.updateBuildRunner(
                        runner,
                        runner,
                        runner,
                        match { updatedMap ->
                            originalRunnerParameters.all { updatedMap.containsKey(it.key) && updatedMap[it.key] == it.value }
                        },
                    )
                }
            }
        }

        @Test
        fun `initializes internal state`() {
            // assert
            verify(exactly = 1) {
                stateStorage.init(
                    parentBuild,
                    eq(
                        DistributedBuildState(
                            listOf(
                                Build("1", listOf(BuildStep("1.1", BuildStepState.Pending))),
                                Build("2", listOf(BuildStep("2.1", BuildStepState.Pending))),
                            ),
                        ),
                    ),
                )
            }
        }
    }

    @Nested
    @DisplayName("on build state change")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnBuildStateChange {
        private val eventBuild = mockk<SBuild>()

        @BeforeEach
        fun setup() {
            clearMocks(eventBuild)

            val parentBuildPromotionId = 0xC00L

            with(eventBuild) {
                hasName("build")
                every { buildPromotion } returns
                    mockk<BuildPromotionEx> {
                        every { getGeneratedById() } returns parentBuildPromotionId
                    }
            }

            with(promotionManager) {
                every { findPromotionById(parentBuildPromotionId) } returns
                    mockk {
                        every { associatedBuild } returns parentBuild
                    }
            }
        }

        private fun `state change test cases`() =
            listOf(
                StateChangeTestCase(
                    currentState =
                        DistributedBuildState(
                            listOf(
                                Build(
                                    "1",
                                    listOf(
                                        BuildStep("1.1", BuildStepState.Pending),
                                        BuildStep("1.2", BuildStepState.Pending),
                                    ),
                                ),
                            ),
                        ),
                    buildEvent = DistributedBuildEvent.BuildStepStarted(eventBuild, "1.1"),
                    expectedUpdate = sequenceOf(BuildStep("1.1", BuildStepState.Running)),
                    expectedStateEvent =
                        DistributedBuildStateChanged.BuildStepStarted(
                            buildId = parentBuildId,
                            buildState = DistributedBuildState(listOf()),
                            stepName = "1.1",
                        ),
                ),
                StateChangeTestCase(
                    currentState =
                        DistributedBuildState(
                            listOf(
                                Build(
                                    "1",
                                    listOf(
                                        BuildStep("1.1", BuildStepState.Running),
                                        BuildStep("1.2", BuildStepState.Pending),
                                    ),
                                ),
                            ),
                        ),
                    buildEvent = DistributedBuildEvent.BuildStepCompleted(eventBuild, "1.1", StepOutcome.Success),
                    expectedUpdate = sequenceOf(BuildStep("1.1", BuildStepState.Completed, StepOutcome.Success)),
                    expectedStateEvent =
                        DistributedBuildStateChanged.BuildStepCompleted(
                            buildId = parentBuildId,
                            buildState = DistributedBuildState(listOf()),
                            "1.1",
                            StepOutcome.Success,
                        ),
                ),
                StateChangeTestCase(
                    currentState =
                        DistributedBuildState(
                            listOf(
                                Build(
                                    "1",
                                    listOf(
                                        BuildStep("1.1", BuildStepState.Running),
                                        BuildStep("1.2", BuildStepState.Pending),
                                    ),
                                ),
                            ),
                        ),
                    buildEvent = DistributedBuildEvent.BuildStepCompleted(eventBuild, "1.1", StepOutcome.Failure),
                    expectedUpdate =
                        sequenceOf(
                            BuildStep("1.1", BuildStepState.Completed, StepOutcome.Failure),
                            BuildStep("1.2", BuildStepState.Skipped),
                        ),
                    expectedStateEvent =
                        DistributedBuildStateChanged.BuildStepCompleted(
                            buildId = parentBuildId,
                            buildState = DistributedBuildState(listOf()),
                            "1.1",
                            StepOutcome.Failure,
                        ),
                ),
                StateChangeTestCase(
                    currentState =
                        DistributedBuildState(
                            listOf(
                                Build("1", listOf(BuildStep("1.1", BuildStepState.Completed, StepOutcome.Failure))),
                                Build(
                                    "2",
                                    listOf(
                                        BuildStep("2.1", BuildStepState.Pending),
                                        BuildStep("2.2", BuildStepState.Pending),
                                    ),
                                ),
                            ),
                        ),
                    buildEvent = DistributedBuildEvent.BuildSkipped(eventBuild),
                    expectedUpdate =
                        sequenceOf(
                            BuildStep("2.1", BuildStepState.Skipped),
                            BuildStep("2.2", BuildStepState.Skipped),
                        ),
                    expectedStateEvent =
                        DistributedBuildStateChanged.BuildSkipped(
                            buildId = parentBuildId,
                            buildState = DistributedBuildState(listOf()),
                            buildName = "2",
                        ),
                    buildName = "2",
                ),
            )

        @ParameterizedTest
        @MethodSource("state change test cases")
        fun `updates corresponding state in the underlying storage`(case: StateChangeTestCase) =
            runTest {
                // arrange
                eventBuild hasName case.buildName
                stateStorage withState case.currentState

                // act
                val result = createInstance().act(case.buildEvent).getOrNull()

                // assert
                result shouldNotBe null
                verify(exactly = 1) {
                    with(any<Raise<Error>>()) {
                        stateStorage.update(parentBuild, match { it.toList() == case.expectedUpdate.toList() })
                    }
                }
            }

        @ParameterizedTest
        @MethodSource("state change test cases")
        fun `sends notification with updated state`(case: StateChangeTestCase) =
            runTest {
                // arrange
                eventBuild hasName case.buildName
                with(stateStorage) {
                    withState(case.currentState)

                    every {
                        with(any<Raise<Error>>()) {
                            update(eventBuild, case.expectedUpdate)
                        }
                    } returns case.expectedStateEvent.buildState
                }

                val tracker = createInstance()

                // act
                val result = tracker.act(case.buildEvent)

                // assert
                result shouldNotBe null
                coVerify { eventBus.dispatch(case.expectedStateEvent) }
            }

        @Test
        fun `disposes underlying storage on build completion`() =
            runTest {
                // arrange
                val event = DistributedBuildEvent.BuildStepCompleted(eventBuild, eventBuild.buildTypeName, StepOutcome.Success)
                stateStorage withState
                    DistributedBuildState(
                        listOf(
                            Build(eventBuild.buildTypeName, listOf(BuildStep("1", BuildStepState.Running))),
                            Build("another build", listOf(BuildStep("1", BuildStepState.Completed, StepOutcome.Success))),
                        ),
                    )

                // act
                val result = createInstance().act(event)

                // assert
                result shouldNotBe null
                verify(exactly = 1) {
                    stateStorage.dispose(parentBuild)
                }
            }

        private suspend fun DistributedBuildStateTracker.act(event: DistributedBuildEvent) = either { handleBuildEvent(event) }
    }

    data class StateChangeTestCase(
        val currentState: DistributedBuildState,
        val buildEvent: DistributedBuildEvent,
        val expectedUpdate: Sequence<BuildStep>,
        val expectedStateEvent: DistributedBuildStateChanged,
        val buildName: String = "1",
    )

    private infix fun SBuild.hasName(name: String) = every { buildTypeName } returns name

    private infix fun DistributedBuildStateStorage.withState(state: DistributedBuildState) =
        every {
            with(any<Raise<Error>>()) {
                get(parentBuild)
            }
        } returns state

    private fun createPromotion(
        id: String = "promotion",
        buildTypeSettings: BuildTypeSettingsEx = mockk<BuildTypeSettingsEx>(),
        runners: Collection<SBuildStepDescriptor> = emptyList(),
    ): BuildPromotionEx =
        mockk<BuildPromotionEx> {
            every { buildSettings } returns
                mockk {
                    every { buildRunners } returns runners
                }
            every { buildType } returns
                mockk {
                    every { settings } returns buildTypeSettings
                    every { name } returns id
                }
        }

    private fun createRunner(
        id: String = "runner",
        parameters: Map<String, String> = emptyMap(),
    ) = mockk<SBuildStepDescriptor> {
        every { this@mockk.id } returns id
        every { name } returns id
        every { type } returns id
        every { this@mockk.parameters } returns parameters
    }

    private fun createInstance() = DistributedBuildStateTracker(promotionManager, stateStorage, eventBus)
}
