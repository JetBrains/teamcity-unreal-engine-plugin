package com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import jetbrains.buildServer.commitPublisher.BasePublisherSettings
import jetbrains.buildServer.commitPublisher.CommitStatusPublisher
import jetbrains.buildServer.commitPublisher.CommitStatusPublisherProblems
import jetbrains.buildServer.commitPublisher.PublisherException
import jetbrains.buildServer.serverSide.BuildTypeIdentity
import jetbrains.buildServer.serverSide.IOGuard
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.WebLinks
import jetbrains.buildServer.util.ssl.SSLTrustStoreProvider
import jetbrains.buildServer.vcs.VcsRoot
import jetbrains.buildServer.web.openapi.PluginDescriptor
import kotlinx.coroutines.runBlocking

class UgsCommitStatusPublisherSettings(
    descriptor: PluginDescriptor,
    links: WebLinks,
    problems: CommitStatusPublisherProblems,
    trustStoreProvider: SSLTrustStoreProvider,
    private val parametersParser: UgsParametersParser,
    private val client: UgsMetadataServerClient,
) : BasePublisherSettings(
        descriptor,
        links,
        problems,
        trustStoreProvider,
    ) {
    companion object {
        val SUPPORTED_EVENTS =
            listOf(
                CommitStatusPublisher.Event.STARTED,
                CommitStatusPublisher.Event.FINISHED,
                CommitStatusPublisher.Event.INTERRUPTED,
                CommitStatusPublisher.Event.FAILURE_DETECTED,
                CommitStatusPublisher.Event.MARKED_AS_SUCCESSFUL,
            )
    }

    override fun getId() = "ugs-metadata-server"

    override fun getName() = "UGS Metadata Server"

    override fun getEditSettingsUrl() = myDescriptor.getPluginResourcesPath("ugsCommitStatusPublisher.jsp")

    override fun isTestConnectionSupported() = true

    override fun testConnection(
        buildTypeOrTemplate: BuildTypeIdentity,
        root: VcsRoot,
        params: MutableMap<String, String>,
    ) = IOGuard.allowNetworkCall<Exception> {
        runBlocking<Unit> {
            parametersParser
                .parse(params)
                .map { (serverUrl, _, _) ->
                    either {
                        client.testConnection(serverUrl)
                    }.onLeft {
                        when (it) {
                            is GenericError -> throw PublisherException(it.message, it.exception)
                            else -> throw PublisherException(
                                "An unexpected error occurred while testing the connection to the metadata server",
                            )
                        }
                    }
                }
        }
    }

    override fun createPublisher(
        buildType: SBuildType,
        buildFeatureId: String,
        params: MutableMap<String, String>,
    ) = UgsCommitStatusPublisher(
        this,
        buildType,
        buildFeatureId,
        myLinks,
        params,
        myProblems,
        UgsCommitStatusPublisher.PublishBadgeCommand(parametersParser, client, myLinks),
    )

    override fun isEventSupported(
        event: CommitStatusPublisher.Event?,
        buildType: SBuildType?,
        params: MutableMap<String, String>?,
    ) = SUPPORTED_EVENTS.contains(event)

    override fun describeParameters(parameters: MutableMap<String, String>): String {
        val (serverUrl, badgeName, project) =
            parametersParser.parse(parameters).getOrElse {
                return super.describeParameters(parameters)
            }

        return "Post the result of this build as a badge \"${badgeName.value}\" " +
            "for project \"${project.value}\" " +
            "on the UGS metadata server \"${serverUrl.value}\""
    }

    override fun getParametersProcessor(unused: BuildTypeIdentity) =
        PropertiesProcessor { parameters ->
            when (val result = parametersParser.parse(parameters)) {
                is Either.Left ->
                    result.value
                        .map { InvalidProperty(it.propertyName, it.message) }
                        .toMutableList()
                is Either.Right -> emptyList()
            }
        }
}
