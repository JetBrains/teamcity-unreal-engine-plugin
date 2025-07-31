package com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.JsonEncoder
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.raise
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

class UgsMetadataServerClient(
    engine: HttpClientEngine,
    private val settings: UgsMetadataServerSettings,
) : AutoCloseable {
    companion object {
        private val logger = UnrealPluginLoggers.get<UgsMetadataServerClient>()
        private val depotPathSplitter = PerforceDepotPathSplitter()

        @JvmStatic
        fun createInstance() = UgsMetadataServerClient(CIO.create(), UgsMetadataServerSettings())
    }

    private val client =
        HttpClient(engine) {
            expectSuccess = true

            install(ContentNegotiation) {
                json(json = JsonEncoder.instance)
            }

            install(Logging) {
                this.logger =
                    object : Logger {
                        override fun log(message: String) = Companion.logger.debug(message)
                    }
            }

            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = settings.retryCount)
                exponentialDelay()
            }

            install(HttpTimeout) {
                requestTimeoutMillis = settings.requestTimeout.inWholeMilliseconds
            }
        }

    context(_: Raise<Error>)
    suspend fun testConnection(url: UgsMetadataServerUrl) = getApiVersion(url)

    context(_: Raise<Error>)
    private suspend fun getApiVersion(url: UgsMetadataServerUrl) =
        performRequest("${url.ensureTrailingSlash()}api/latest") {
            method = HttpMethod.Get
            url {
                // RUGS fails if we don't specify one (even a non-existing one)
                parameters.append("project", "//depot/stream/project")
            }
        }.body<LatestData?>()?.version ?: 1

    context(_: Raise<Error>)
    suspend fun postBuildMetadata(
        url: UgsMetadataServerUrl,
        metadata: UgsBuildMetadata,
    ) {
        val version = getApiVersion(url)
        when (version) {
            1 -> postBuildMetadataV1(url, metadata)
            else -> postBuildMetadataV2(url, metadata)
        }
    }

    context(_: Raise<Error>)
    private suspend fun postBuildMetadataV1(
        url: UgsMetadataServerUrl,
        metadata: UgsBuildMetadata,
    ) {
        val endpoint = "${url.ensureTrailingSlash()}api/build"

        val v1Request =
            AddUgsMetadataRequestV1(
                change = metadata.change,
                project = metadata.projectDirectory.value,
                badgeName = metadata.badgeName,
                url = metadata.url,
                badgeState = metadata.badgeState,
            )

        performRequest(endpoint) {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(v1Request)
        }
    }

    context(_: Raise<Error>)
    private suspend fun postBuildMetadataV2(
        url: UgsMetadataServerUrl,
        metadata: UgsBuildMetadata,
    ) {
        val (stream, project) = depotPathSplitter.split(metadata.projectDirectory)

        val badges =
            listOf(
                AddUgsBadgeRequest(
                    name = metadata.badgeName,
                    url = metadata.url,
                    state = metadata.badgeState,
                ),
            )

        val v2Request =
            AddUgsMetadataRequestV2(
                stream = stream.value,
                change = metadata.change,
                project = project.value,
                badges = badges,
            )

        val endpoint = "${url.ensureTrailingSlash()}api/metadata"
        performRequest(endpoint) {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(v2Request)
        }
    }

    context(_: Raise<Error>)
    private suspend fun performRequest(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse =
        client
            .runCatching {
                request(url) {
                    block(this)
                }
            }.getOrElse {
                raise("An unexpected error occurred while communicating with metadata server \"$url\"", it)
            }

    override fun close() = client.close()

    private fun UgsMetadataServerUrl.ensureTrailingSlash() = if (value.endsWith("/")) value else "$value/"
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class LatestData(
    @JsonNames("Version")
    val version: Int? = null,
    @SerialName("LastEventId")
    val lastEventId: Long = 0,
    @SerialName("LastCommentId")
    val lastCommentId: Long = 0,
    @SerialName("LastBuildId")
    val lastBuildId: Long = 0,
)
