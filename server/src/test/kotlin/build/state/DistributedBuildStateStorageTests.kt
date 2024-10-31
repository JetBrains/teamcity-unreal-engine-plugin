package build.state

import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.build.events.StepOutcome
import com.jetbrains.teamcity.plugins.unrealengine.server.build.getUnrealDataStorage
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildState
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildState.*
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateStorage
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import jetbrains.buildServer.serverSide.CustomDataStorage
import jetbrains.buildServer.serverSide.SBuild
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DistributedBuildStateStorageTests {
    private val build = mockk<SBuild>()
    private val storage = mockk<CustomDataStorage>()

    init {
        mockkStatic(SBuild::getUnrealDataStorage)
    }

    @BeforeEach
    fun init() {
        clearAllMocks()

        with(build) {
            every { getUnrealDataStorage(any()) } returns storage
            every { buildId } returns 0xC00L
        }

        with(storage) {
            every { putValues(any()) } just Runs
            every { updateValues(any(), any()) } just Runs
        }
    }

    data class InitTestCase(
        val state: DistributedBuildState,
        val expectedMap: Map<String, String>,
    )

    private fun `initiates state cases`() =
        listOf(
            InitTestCase(
                DistributedBuildState(
                    listOf(
                        Build(
                            "1",
                            listOf(
                                BuildStep("1.1", BuildStepState.Pending),
                                BuildStep("1.2", BuildStepState.Pending),
                            ),
                        ),
                        Build(
                            "2",
                            listOf(
                                BuildStep("2.1", BuildStepState.Completed, StepOutcome.Success),
                            ),
                        ),
                    ),
                ),
                expectedMap =
                    mapOf(
                        "builds.0.name" to "1",
                        "builds.0.steps.0.name" to "1.1",
                        "builds.0.steps.0.state" to "pending",
                        "builds.0.steps.1.name" to "1.2",
                        "builds.0.steps.1.state" to "pending",
                        "builds.1.name" to "2",
                        "builds.1.steps.0.name" to "2.1",
                        "builds.1.steps.0.state" to "completed",
                        "builds.1.steps.0.outcome" to "success",
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("initiates state cases")
    fun `initiates state`(case: InitTestCase) {
        // act
        DistributedBuildStateStorage().init(build, case.state)

        // assert
        verify {
            storage.putValues(
                match {
                    it shouldContainExactly case.expectedMap
                    true
                },
            )
        }
    }

    data class ReturnTestCase(
        val map: Map<String, String>,
        val expectedState: DistributedBuildState,
    )

    private fun `returns state cases`() =
        listOf(
            ReturnTestCase(
                mapOf(
                    "builds.0.name" to "1",
                    "builds.0.steps.0.name" to "1.1",
                    "builds.0.steps.0.state" to "pending",
                    "builds.0.steps.1.name" to "1.2",
                    "builds.0.steps.1.state" to "pending",
                    "builds.1.name" to "2",
                    "builds.1.steps.0.name" to "2.1",
                    "builds.1.steps.0.state" to "completed",
                    "builds.1.steps.0.outcome" to "success",
                ),
                DistributedBuildState(
                    listOf(
                        Build(
                            "1",
                            listOf(
                                BuildStep("1.1", BuildStepState.Pending),
                                BuildStep("1.2", BuildStepState.Pending),
                            ),
                        ),
                        Build(
                            "2",
                            listOf(
                                BuildStep("2.1", BuildStepState.Completed, StepOutcome.Success),
                            ),
                        ),
                    ),
                ),
            ),
        )

    @ParameterizedTest
    @MethodSource("returns state cases")
    fun `returns state`(case: ReturnTestCase) {
        // arrange
        every { storage.values } returns case.map

        // act
        val state = either { DistributedBuildStateStorage().get(build) }.getOrNull()

        // assert
        state shouldNotBe null
        state shouldBe case.expectedState
    }

    data class UpdateTestCase(
        val updates: Sequence<BuildStep>,
        val currentState: Map<String, String>,
        val expectedUpdate: Map<String, String>,
    )

    private fun `performs partial update cases`() =
        listOf(
            UpdateTestCase(
                updates =
                    sequenceOf(
                        BuildStep("1.2", BuildStepState.Completed, StepOutcome.Failure),
                        BuildStep("2.1", BuildStepState.Running),
                    ),
                currentState =
                    mapOf(
                        "builds.0.name" to "1",
                        "builds.0.steps.0.name" to "1.1",
                        "builds.0.steps.0.state" to "completed",
                        "builds.1.steps.0.outcome" to "success",
                        "builds.0.steps.1.name" to "1.2",
                        "builds.0.steps.1.state" to "running",
                        "builds.1.name" to "2",
                        "builds.1.steps.0.name" to "2.1",
                        "builds.1.steps.0.state" to "pending",
                    ),
                expectedUpdate =
                    mapOf(
                        "builds.0.steps.1.name" to "1.2",
                        "builds.0.steps.1.state" to "completed",
                        "builds.0.steps.1.outcome" to "failure",
                        "builds.1.steps.0.name" to "2.1",
                        "builds.1.steps.0.state" to "running",
                    ),
            ),
        )

    @ParameterizedTest
    @MethodSource("performs partial update cases")
    fun `performs partial update`(case: UpdateTestCase) {
        // arrange
        every { storage.values } returns case.currentState

        // act
        val result = either { DistributedBuildStateStorage().update(build, case.updates) }.getOrNull()

        // assert
        result shouldNotBe null
        verify {
            storage.updateValues(
                match {
                    it shouldContainExactly case.expectedUpdate
                    true
                },
                emptySet(),
            )
        }
    }
}
