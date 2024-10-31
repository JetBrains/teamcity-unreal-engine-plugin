package build.status.ugs

import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.BadgeState
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsBuildMetadata
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsMetadataServerClient
import com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs.UgsMetadataServerSettings
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UgsMetadataServerClientTests {
    private val ugsServerUrl = UgsMetadataServerUrl("http://localhost:1111/ugs-metadata-server")

    @Test
    fun `tests connection`() =
        runTest {
            // arrange
            var requestUrl = ""
            val mockEngine =
                MockEngine {
                    requestUrl = it.url.toString()
                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                    )
                }

            val metadataServerClient = UgsMetadataServerClient(mockEngine, UgsMetadataServerSettings())

            // act
            val result = either { metadataServerClient.testConnection(ugsServerUrl) }

            // assert
            assertTrue(result.isRight())
            assertEquals("${ugsServerUrl.value}/api/latest?project=%2F%2Fdepot%2Fstream%2Fproject", requestUrl)
        }

    @Test
    fun `publishes badge`() =
        runTest {
            // arrange
            var requestUrl = ""
            var requestBody = ""

            val mockEngine =
                MockEngine {
                    requestUrl = it.url.toString()
                    requestBody = String(it.body.toByteArray())
                    respond(
                        content = "",
                        status = HttpStatusCode.OK,
                    )
                }

            val metadataServerClient = UgsMetadataServerClient(mockEngine, UgsMetadataServerSettings())
            val metadata =
                UgsBuildMetadata(
                    111L,
                    "//depot/stream/project",
                    "foo",
                    "http://link-to-build-log",
                    BadgeState.Success,
                )

            // act
            val result = either { metadataServerClient.postBuildMetadata(ugsServerUrl, metadata) }

            // assert
            assertTrue(result.isRight())
            assertEquals("${ugsServerUrl.value}/api/build", requestUrl)
            assertEquals(
                """
                {"ChangeNumber":111,"Project":"//depot/stream/project","BuildType":"foo","Url":"http://link-to-build-log","Result":3}
                """.trimIndent(),
                requestBody,
            )
        }
}
