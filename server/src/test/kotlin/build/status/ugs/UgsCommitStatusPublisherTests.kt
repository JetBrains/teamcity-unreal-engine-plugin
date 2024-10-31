package build.status.ugs

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.PropertyValidationError
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.BadgeState
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsBadgeName
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsBuildFeatureParameters
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsBuildMetadata
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsCommitStatusPublisher
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsMetadataServerClient
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsParametersParser
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsProject
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.getPerforceChangelistNumber
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import jetbrains.buildServer.commitPublisher.CommitStatusPublisher
import jetbrains.buildServer.serverSide.BuildRevision
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.serverSide.SBuild
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UgsCommitStatusPublisherTests {
    private val client = mockk<UgsMetadataServerClient>()
    private val parametersParser = mockk<UgsParametersParser>()
    private val links = mockk<RelativeWebLinks>()
    private val buildRevision = mockk<BuildRevision>()
    private val build = mockk<SBuild>()
    private val ugsServerUrl = UgsMetadataServerUrl("http://ugsapi-server.net")

    private val viewLink = "view link"
    private val buildFeatureParameters =
        UgsBuildFeatureParameters(
            ugsServerUrl,
            UgsBadgeName("badge"),
            UgsProject("//depot/stream/project"),
        )

    init {
        mockkStatic(BuildRevision::getPerforceChangelistNumber)
    }

    @BeforeEach
    fun init() {
        clearAllMocks()

        every { links.getViewResultsUrl(any()) } returns viewLink

        with(buildRevision) {
            withRootName("perforce")
            every { revision } returns "123"
            every { getPerforceChangelistNumber() } returns 123L
        }

        every { parametersParser.parse(any()) } returns Either.Right(buildFeatureParameters)

        coEvery {
            with(any<Raise<Error>>()) {
                client.postBuildMetadata(ugsServerUrl, any())
            }
        } returns Unit
    }

    @Test
    fun `publishes badge`() =
        runTest {
            // act
            val result = act(event = CommitStatusPublisher.Event.STARTED).getOrNull()

            // assert
            result shouldNotBe null
            coVerify(exactly = 1) {
                with(any<Raise<Error>>()) {
                    client.postBuildMetadata(
                        ugsServerUrl,
                        UgsBuildMetadata(
                            change = 123L,
                            project = buildFeatureParameters.project.value,
                            badgeName = buildFeatureParameters.badgeName.value,
                            url = viewLink,
                            badgeState = BadgeState.Starting,
                        ),
                    )
                }
            }
            confirmVerified(client)
        }

    @Test
    fun `does not publish badge for non-Perforce vcs root`() =
        runTest {
            // arrange
            buildRevision.withRootName("git")

            // act
            val result = act().leftOrNull()

            // assert
            result shouldNotBe null
            result.shouldBeTypeOf<GenericError>()
            verify { client wasNot Called }
        }

    @Test
    fun `does not publish badge when parameters are invalid`() =
        runTest {
            // arrange
            every { parametersParser.parse(any()) } returns
                Either.Left(
                    nonEmptyListOf(PropertyValidationError("foo", "bar")),
                )

            // act
            val result = act().leftOrNull()

            // assert
            result shouldNotBe null
            result.shouldBeTypeOf<GenericError>()
            verify { client wasNot Called }
        }

    @Test
    fun `does not publish badge when it cannot determine the change number`() =
        runTest {
            // arrange
            every { buildRevision.getPerforceChangelistNumber() } returns null

            // act
            val result = act().leftOrNull()

            // assert
            result shouldNotBe null
            result.shouldBeTypeOf<GenericError>()
            verify { client wasNot Called }
        }

    private suspend fun act(event: CommitStatusPublisher.Event = CommitStatusPublisher.Event.STARTED) =
        either {
            UgsCommitStatusPublisher
                .PublishBadgeCommand(
                    parametersParser,
                    client,
                    links,
                ).execute(event, buildRevision, build, emptyMap())
        }

    private fun BuildRevision.withRootName(name: String) {
        every { root } returns
            mockk {
                every { vcsName } returns name
            }
    }
}
