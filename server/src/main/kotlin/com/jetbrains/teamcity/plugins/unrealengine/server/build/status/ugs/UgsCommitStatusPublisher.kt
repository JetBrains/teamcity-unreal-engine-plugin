package com.jetbrains.teamcity.plugins.unrealengine.server.build.status.ugs

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.jetbrains.teamcity.plugins.unrealengine.common.Error
import com.jetbrains.teamcity.plugins.unrealengine.common.GenericError
import com.jetbrains.teamcity.plugins.unrealengine.common.UnrealPluginLoggers
import com.jetbrains.teamcity.plugins.unrealengine.common.ensure
import com.jetbrains.teamcity.plugins.unrealengine.common.ensureNotNull
import com.jetbrains.teamcity.plugins.unrealengine.common.raise
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.getPerforceChangelistNumber
import com.jetbrains.teamcity.plugins.unrealengine.server.extensions.logResult
import jetbrains.buildServer.commitPublisher.CommitStatusPublisher
import jetbrains.buildServer.commitPublisher.CommitStatusPublisherProblems
import jetbrains.buildServer.commitPublisher.CommitStatusPublisherSettings
import jetbrains.buildServer.commitPublisher.HttpBasedCommitStatusPublisher
import jetbrains.buildServer.commitPublisher.PublisherException
import jetbrains.buildServer.serverSide.BuildRevision
import jetbrains.buildServer.serverSide.IOGuard
import jetbrains.buildServer.serverSide.RelativeWebLinks
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.WebLinks
import jetbrains.buildServer.swarm.SwarmClientManager
import kotlinx.coroutines.runBlocking

class UgsCommitStatusPublisher(
    settings: CommitStatusPublisherSettings,
    buildType: SBuildType,
    buildFeatureId: String,
    webLinks: WebLinks,
    parameters: Map<String, String>,
    problems: CommitStatusPublisherProblems,
    private val publishBadgeCommand: PublishBadgeCommand,
) : HttpBasedCommitStatusPublisher<BadgeState>(
        settings,
        buildType,
        buildFeatureId,
        parameters,
        problems,
        webLinks,
    ) {
    companion object {
        private val logger = UnrealPluginLoggers.get<UgsCommitStatusPublisher>()
    }

    val parameters: Map<String, String> = myParams

    override fun getId() = settings.id

    override fun toString() = settings.name

    override fun buildStarted(
        build: SBuild,
        revision: BuildRevision,
    ) = publishBadge(CommitStatusPublisher.Event.STARTED, revision, build)

    override fun buildInterrupted(
        build: SBuild,
        revision: BuildRevision,
    ) = publishBadge(CommitStatusPublisher.Event.INTERRUPTED, revision, build)

    override fun buildFinished(
        build: SBuild,
        revision: BuildRevision,
    ) = publishBadge(CommitStatusPublisher.Event.FINISHED, revision, build)

    override fun buildFailureDetected(
        build: SBuild,
        revision: BuildRevision,
    ) = publishBadge(CommitStatusPublisher.Event.FAILURE_DETECTED, revision, build)

    override fun buildMarkedAsSuccessful(
        build: SBuild,
        revision: BuildRevision,
        buildInProgress: Boolean,
    ) = publishBadge(CommitStatusPublisher.Event.MARKED_AS_SUCCESSFUL, revision, build)

    private fun publishBadge(
        event: CommitStatusPublisher.Event,
        revision: BuildRevision,
        build: SBuild,
    ): Boolean =
        IOGuard.allowNetworkCall<Boolean, Exception> {
            runBlocking {
                either {
                    publishBadgeCommand.execute(event, revision, build, parameters)
                }.also {
                    logger.logResult(it, "badge state publication")
                }.mapLeft {
                    when (it) {
                        is GenericError -> throw PublisherException(it.message, it.exception)
                        else -> throw PublisherException(
                            "An unexpected error occurred during build status publication to the UGS metadata server",
                        )
                    }
                }.isRight()
            }
        }

    // This level of indirection exists solely for testing purposes,
    // as it is impossible to create the base class HttpBasedCommitStatusPublisher in tests due to its core dependencies
    class PublishBadgeCommand(
        private val parametersParser: UgsParametersParser,
        private val metadataServerClient: UgsMetadataServerClient,
        private val links: RelativeWebLinks,
    ) {
        context(Raise<Error>)
        suspend fun execute(
            event: CommitStatusPublisher.Event,
            revision: BuildRevision,
            build: SBuild,
            parameters: Map<String, String>,
        ) {
            val state =
                when (event) {
                    CommitStatusPublisher.Event.STARTED -> BadgeState.Starting
                    CommitStatusPublisher.Event.FINISHED -> if (build.buildStatus.isSuccessful) BadgeState.Success else BadgeState.Failure
                    CommitStatusPublisher.Event.INTERRUPTED -> BadgeState.Skipped
                    CommitStatusPublisher.Event.FAILURE_DETECTED -> BadgeState.Failure
                    CommitStatusPublisher.Event.MARKED_AS_SUCCESSFUL -> BadgeState.Success
                    else -> raise("Received unsupported event type: $event")
                }

            ensure(
                revision.root.vcsName == SwarmClientManager.PERFORCE_VCS_NAME,
                "Attempt to publish UGS badge for a non-Perforce VCS root. Skipping publication since UGS is specific to Perforce",
            )

            val change =
                ensureNotNull(
                    revision.getPerforceChangelistNumber(),
                    "Unable to parse Perforce changelist number from ${revision.revision}",
                )

            val (metadataServerUrl, badgeName, project) =
                parametersParser.parse(parameters).getOrElse { errors ->
                    raise(
                        """
                        Unable to parse parameters from the build feature.
                        ${errors.joinToString(separator = ", ") { it.propertyName + ": " + it.message }}
                        """.trimIndent(),
                    )
                }

            val metadata =
                UgsBuildMetadata(
                    change = change,
                    badgeName = badgeName.value,
                    badgeState = state,
                    url = links.getViewResultsUrl(build),
                    projectDirectory = PerforceDepotPath(project.value),
                )

            metadataServerClient.postBuildMetadata(metadataServerUrl, metadata)
        }
    }
}
