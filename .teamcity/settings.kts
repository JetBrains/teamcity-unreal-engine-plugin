import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinFile
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.version

version = "2024.03"

project {
    vcsRoot(TagReleaseVcs)
    vcsRoot(PullRequestVcs)
    vcsRoot(MasterVcs)

    buildType(ReleaseBuildConfiguration)
    buildType(PullRequestBuildConfiguration)
    buildType(MasterBuildConfiguration)
}

object TagReleaseVcs : GitVcsRoot({
    id("TagReleaseVcs")
    name = "TagReleaseVcs"
    branch = "master"
    useTagsAsBranches = true
    branchSpec = """
        +:refs/(tags/*)
        -:<default>
    """.trimIndent()
    url = "https://github.com/JetBrains/teamcity-unreal-engine-plugin.git"
})

object ReleaseBuildConfiguration : BuildType({
    id("ReleaseBuild")
    name = "ReleaseBuild"

    params {
        password("env.ORG_GRADLE_PROJECT_jetbrains.marketplace.token", "credentialsJSON:f5d30123-5324-4032-b9d0-aa8b5bf6c6d5", readOnly = true)
    }

    vcs {
        root(TagReleaseVcs)
    }

    steps {
        kotlinFile {
            name = "calculate version"
            path = "./.teamcity/steps/calculateVersion.kts"
            arguments = "%teamcity.build.branch%"
        }
        gradle {
            name = "build"
            tasks = "clean build serverPlugin"
        }
        gradle {
            name = "publish to marketplace"
            tasks = "publishPlugin"
        }
    }

    artifactRules = "+:./server/build/distributions/teamcity-unreal-engine-plugin-server.zip"

    triggers {
        vcs {
            branchFilter = "+:tags/v*"
        }
    }
})

object PullRequestVcs : GitVcsRoot({
    id("PullRequestVcs")
    name = "PullRequestVcs"
    branch = "refs/heads/master"
    branchSpec = """
        -:<default>
    """.trimIndent()
    url = "https://github.com/JetBrains/teamcity-unreal-engine-plugin.git"
})

object PullRequestBuildConfiguration : BuildType({
    id("PullRequestBuild")
    name = "PullRequestBuild"

    val githubTokenParameter = "GITHUB_TOKEN"
    params {
        password(githubTokenParameter, "credentialsJSON:9836dc1a-9546-4018-b7c5-b5cbbaa03213", readOnly = true)
    }

    vcs {
        root(PullRequestVcs)
    }

    triggers {
        vcs {
            branchFilter = """
                +:*
                -:<default>
            """.trimIndent()
        }
    }

    features {
        pullRequests {
            vcsRootExtId = PullRequestVcs.id?.value
            provider = github {
                filterTargetBranch = "refs/heads/master"
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER_OR_COLLABORATOR
                authType = token {
                    token = "%$githubTokenParameter%"
                }
            }
        }
        commitStatusPublisher {
            vcsRootExtId = PullRequestVcs.id?.value
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "%$githubTokenParameter%"
                }
            }
        }
    }

    steps {
        gradle {
            name = "build"
            tasks = "clean build serverPlugin"
        }
    }
})

object MasterVcs : GitVcsRoot({
    id("MasterVcs")
    name = "MasterVcs"
    branch = "refs/heads/master"
    url = "https://github.com/JetBrains/teamcity-unreal-engine-plugin.git"
})

object MasterBuildConfiguration : BuildType({
    id("MasterBuild")
    name = "MasterBuild"

    allowExternalStatus = true

    vcs {
        root(MasterVcs)
    }

    triggers {
        vcs {
            branchFilter = "+:*"
        }
    }

    steps {
        gradle {
            name = "build"
            tasks = "clean build serverPlugin"
        }
    }
})
