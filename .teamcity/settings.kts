import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinFile
import jetbrains.buildServer.configs.kotlin.project
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.version

version = "2026.1"

project {
    buildType(ReleaseBuildConfiguration)
    buildType(MasterBuildConfiguration)
}

object ReleaseBuildConfiguration : BuildType({
    id("ReleaseBuild")
    name = "Unreal Plugin: release build"

    params {
        password("env.ORG_GRADLE_PROJECT_jetbrains.marketplace.token", "credentialsJSON:f5d30123-5324-4032-b9d0-aa8b5bf6c6d5", readOnly = true)
    }

    vcs {
        root(DslContext.settingsRoot)
        branchFilter = "+:tags/*"
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

object MasterBuildConfiguration : BuildType({
    id("MasterBuild")
    name = "Unreal Plugin: master and PRs build"

    allowExternalStatus = true

    val githubTokenParameter = "GITHUB_TOKEN"
    params {
        password(githubTokenParameter, "credentialsJSON:c2a1efe4-4a6e-4908-a12b-f61147f8028d", readOnly = true)
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    triggers {
        vcs {
            branchFilter = "+:*"
        }
    }

    features {
        pullRequests {
            vcsRootExtId = DslContext.settingsRoot.id?.value
            provider = github {
                filterTargetBranch = "refs/heads/master"
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER_OR_COLLABORATOR
                authType = token {
                    token = "%$githubTokenParameter%"
                }
            }
        }
        commitStatusPublisher {
            vcsRootExtId = DslContext.settingsRoot.id?.value
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
