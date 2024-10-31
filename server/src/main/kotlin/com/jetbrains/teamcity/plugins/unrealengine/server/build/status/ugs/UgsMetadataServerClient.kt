package com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs

import arrow.core.raise.Raise
import com.jetbrains.teamcity.plugins.framework.common.TeamCityLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.JsonEncoder
import com.jetbrains.teamcity.plugins.unrealengine.common.raise
import com.jetbrains.teamcity.plugins.unrealengine.common.ugs.UgsMetadataServerUrl
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

class UgsMetadataServerClient(
    engine: HttpClientEngine,
    private val settings: UgsMetadataServerSettings,
) : AutoCloseable {
    companion object {
        private val logger = TeamCityLoggers.server<UgsMetadataServerClient>()

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

    context(Raise<Error>)
    suspend fun testConnection(url: UgsMetadataServerUrl) =
        performRequest("${url.ensureTrailingSlash()}api/latest") {
            method = HttpMethod.Get
            url {
                // RUGS fails if we don't specify one (even a non-existing one)
                parameters.append("project", "//depot/stream/project")
            }
        }

    context(Raise<Error>)
    suspend fun postBuildMetadata(
        url: UgsMetadataServerUrl,
        metadata: UgsBuildMetadata,
    ) = performRequest("${url.ensureTrailingSlash()}api/build") {
        method = HttpMethod.Post
        contentType(ContentType.Application.Json)
        setBody(metadata)
    }

    context(Raise<Error>)
    private suspend fun performRequest(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): Unit =
        client
            .runCatching {
                request(url) {
                    block(this)
                }
                Unit
            }.getOrElse {
                raise("An unexpected error occurred while communicating with metadata server \"$url\"", it)
            }

    override fun close() = client.close()

    private fun UgsMetadataServerUrl.ensureTrailingSlash() = if (value.endsWith("/")) value else "$value/"
}
