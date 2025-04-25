package build.status.ugs

import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.BadgeState
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.PerforceDepotPath
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsBuildMetadata
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsMetadataServerClient
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsMetadataServerSettings
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UgsMetadataServerClientTests {
    private val ugsServerUrl = UgsMetadataServerUrl("http://localhost:1111/ugs-metadata-server")

    @Test
    fun `tests connection`() =
        runTest {
            // arrange
            val mockEngine = createMockEngine()
            val metadataServerClient = UgsMetadataServerClient(mockEngine, UgsMetadataServerSettings())

            // act
            val result = either { metadataServerClient.testConnection(ugsServerUrl) }

            // assert
            result.isRight() shouldBe true
            mockEngine.requestHistory.size shouldBe 1
            val request = mockEngine.requestHistory.first()
            request.url.toString() shouldBe "${ugsServerUrl.value}/api/latest?project=%2F%2Fdepot%2Fstream%2Fproject"
        }

    @Test
    fun `publishes badge (V1 Metadata Server)`() =
        runTest {
            // arrange
            val mockEngine = createMockEngine(versionResponse = "{}")
            val client = UgsMetadataServerClient(mockEngine, UgsMetadataServerSettings())
            val metadata =
                UgsBuildMetadata(
                    change = 111L,
                    projectDirectory = PerforceDepotPath("//depot/stream/project"),
                    badgeName = "foo",
                    url = "http://link-to-build-log",
                    badgeState = BadgeState.Success,
                )

            // act
            val result = either { client.postBuildMetadata(ugsServerUrl, metadata) }

            // assert
            result.isRight() shouldBe true
            mockEngine.requestHistory.size shouldBe 2

            val versionRequest = mockEngine.requestHistory.first()
            versionRequest.url.toString() shouldBe "${ugsServerUrl.value}/api/latest?project=%2F%2Fdepot%2Fstream%2Fproject"

            val postMetadataRequest = mockEngine.requestHistory.last()
            postMetadataRequest.url.toString() shouldBe "${ugsServerUrl.value}/api/build"
            postMetadataRequest.body.toByteArray().decodeToString() shouldBe
                """{"ChangeNumber":111,"Project":"//depot/stream/project","BuildType":"foo","Url":"http://link-to-build-log","Result":3}"""
        }

    @Test
    fun `publishes badge (V2 Metadata Server)`() =
        runTest {
            // arrange
            val mockEngine = createMockEngine(versionResponse = """{"Version":2}""")

            val client = UgsMetadataServerClient(mockEngine, UgsMetadataServerSettings())
            val metadata =
                UgsBuildMetadata(
                    change = 111L,
                    projectDirectory = PerforceDepotPath("//depot/stream/project"),
                    badgeName = "foo",
                    url = "http://link-to-build-log",
                    badgeState = BadgeState.Success,
                )

            // act
            val result = either { client.postBuildMetadata(ugsServerUrl, metadata) }

            // assert
            result.isRight() shouldBe true
            mockEngine.requestHistory.size shouldBe 2

            val versionRequest = mockEngine.requestHistory.first()
            versionRequest.url.toString() shouldBe "${ugsServerUrl.value}/api/latest?project=%2F%2Fdepot%2Fstream%2Fproject"

            val postMetadataRequest = mockEngine.requestHistory.last()
            postMetadataRequest.url.toString() shouldBe "${ugsServerUrl.value}/api/metadata"
            postMetadataRequest.body.toByteArray().decodeToString() shouldBe
                """{"stream":"//depot/stream","change":111,"project":"project","badges":[{"name":"foo","url":"http://link-to-build-log","state":3}]}"""
        }

    private fun createMockEngine(versionResponse: String = ""): MockEngine =
        MockEngine { request ->
            when {
                request.url.encodedPath.contains("api/latest") -> {
                    respond(
                        content = versionResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
                else -> {
                    respond("", HttpStatusCode.OK)
                }
            }
        }
}
