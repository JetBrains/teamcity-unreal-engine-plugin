package build.state

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.DistributedBuildStateTracker
import com.jetbrains.teamcity.plugins.unrealengine.server.build.state.SkippedBuildMonitor
import com.jetbrains.teamcity.plugins.unrealengine.server.buildgraph.getGeneratedById
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.isMainNode
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
import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.messages.ErrorData
import jetbrains.buildServer.serverSide.BuildEx
import jetbrains.buildServer.serverSide.BuildPromotionEx
import jetbrains.buildServer.serverSide.SFinishedBuild
import jetbrains.buildServer.serverSide.ServerResponsibility
import jetbrains.buildServer.serverSide.problems.BuildProblem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SkippedBuildMonitorTests {
    private val stateTracker = mockk<DistributedBuildStateTracker>()
    private val serverResponsibility = mockk<ServerResponsibility>()

    private val build = mockk<SFinishedBuild>(moreInterfaces = arrayOf(BuildEx::class))

    init {
        mockkStatic(BuildPromotionEx::getGeneratedById)
        mockkStatic(ServerResponsibility::isMainNode)
    }

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { serverResponsibility.isMainNode() } returns true

        coEvery {
            with(any<Raise<Error>>()) {
                stateTracker.handleBuildEvent(any())
            }
        } just Runs

        with(build as BuildEx) {
            every { buildId } returns 123L
            every { canceledInfo } returns null
            every { isStartedOnAgent } returns false
            (this as SFinishedBuild).asGeneratedBy(0xC00L)
            withProblems(listOf(ErrorData.SNAPSHOT_DEPENDENCY_ERROR_TYPE))
        }
    }

    @Test
    fun `notifies when a build is cancelled due to failed dependencies`() {
        // arrange
        with(build as BuildEx) {
            every { isStartedOnAgent } returns false
            withProblems(listOf(ErrorData.SNAPSHOT_DEPENDENCY_ERROR_TYPE))
        }

        // act
        createInstance().entryCreated(build)

        // assert
        coVerify {
            with(any<Raise<Error>>()) {
                stateTracker.handleBuildEvent(match { it.build == build })
            }
        }
    }

    @Test
    fun `notifies when a build is cancelled due to manual intervention and has not yet started`() {
        // arrange
        with(build as BuildEx) {
            every { isStartedOnAgent } returns false
            every { canceledInfo } returns mockk()
            withProblems(emptyList())
        }

        // act
        createInstance().entryCreated(build)

        // assert
        coVerify {
            with(any<Raise<Error>>()) {
                stateTracker.handleBuildEvent(match { it.build == build })
            }
        }
    }

    @Test
    fun `does not notify when a build is cancelled due to manual intervention if it has already started`() {
        // arrange
        with(build as BuildEx) {
            every { isStartedOnAgent } returns true
            every { canceledInfo } returns mockk()
            withProblems(emptyList())
        }

        // act
        createInstance().entryCreated(build)

        // assert
        confirmVerified(stateTracker)
    }

    @Test
    fun `does not notify about skipped build when not on main node`() {
        // arrange
        every { serverResponsibility.isMainNode() } returns false

        // act
        createInstance().entryCreated(build)

        // assert
        coVerify { stateTracker wasNot Called }
    }

    private fun `not skipped builds`(): Collection<Collection<String>> =
        listOf(
            emptyList(),
            listOf(ErrorData.BUILD_RUNNER_ERROR_TYPE),
            listOf(ErrorData.UNKNOWN_TYPE, ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE),
        )

    @ParameterizedTest
    @MethodSource("not skipped builds")
    fun `ignores build if it's not a skipped one`(problems: Collection<String>) {
        (build as BuildEx).withProblems(problems)

        // act
        createInstance().entryCreated(build)

        // assert
        coVerify { stateTracker wasNot Called }
    }

    @Test
    fun `ignores build if unable to get generator build id`() {
        // arrange
        build.asGeneratedBy(null)

        // act
        createInstance().entryCreated(build)

        // assert
        coVerify { stateTracker wasNot Called }
    }

    private fun SFinishedBuild.asGeneratedBy(id: Long?) {
        every { buildPromotion } returns
            mockk<BuildPromotionEx> {
                every { getGeneratedById() } returns id
            }
    }

    private fun BuildEx.withProblems(problems: Collection<String>) {
        every { buildProblems } returns
            problems.map {
                mockk<BuildProblem> {
                    every { buildProblemData } returns
                        mockk<BuildProblemData> {
                            every { type } returns it
                        }
                }
            }
    }

    private fun createInstance() = SkippedBuildMonitor(serverResponsibility, stateTracker)
}
