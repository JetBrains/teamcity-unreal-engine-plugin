plugins {
    id("plugin.common")
    id(libs.plugins.teamcity.server.get().pluginId)
    id(libs.plugins.teamcity.environments.get().pluginId)
    alias(libs.plugins.changelog)
    alias(libs.plugins.kotlin.serialization)
}

changelog {
    path.set(file("../CHANGELOG.md").canonicalPath)
    groups.set(listOf("Added", "Changed", "Fixed"))
}

teamcity {
    server {
        descriptor {
            name = "unreal-engine"
            displayName = "Unreal Engine Support"
            description = "Provides build facilities for Unreal Engine projects"
            version = project.version.toString()
            vendorName = "JetBrains"
            vendorUrl = "https://www.jetbrains.com/"

            useSeparateClassloader = true
            allowRuntimeReload = true
            nodeResponsibilitiesAware = true

            // virtual configurations functionality is available starting from this version
            minimumBuild = "129203" // 2023.05

            // temporary fragile workaround https://youtrack.jetbrains.com/issue/TW-89103
            dependencies {
                plugin("commit-status-publisher")
            }
        }

        files {
            into("kotlin-dsl") {
                from("${rootProject.projectDir}/kotlin-dsl")
            }
        }

        publish {
            token = project.findProperty("jetbrains.marketplace.token").toString()
            notes = changelog.renderItem(changelog.getLatest(), org.jetbrains.changelog.Changelog.OutputType.HTML)
        }

        archiveName = "${project.parent?.name}-${project.name}"
    }

    environments {
        downloadsDir = "teamcity/downloads"
        baseHomeDir = "teamcity/environments"
        baseDataDir = "teamcity/data"

        create(teamcity.version) {
            version = teamcity.version
            homeDir = "${environments.baseHomeDir}/${teamcity.version}"
        }

    }
}

abstract class BuildFrontendTask
    @Inject constructor(private val operations: ExecOperations)
: DefaultTask() {
    @TaskAction
    fun doTaskAction() {
        operations.exec {
            workingDir(project.file("frontend"))
            commandLine("docker", "build", "-f", "./build.Dockerfile", "-t", "unreal-runner-frontend-build", ".")
        }
        operations.exec { commandLine("docker", "run", "--name", "unreal-runner-frontend-build", "unreal-runner-frontend-build") }
        operations.exec { commandLine("docker", "cp", "unreal-runner-frontend-build:/app/dist/.", "./src/main/resources/buildServerResources/react") }
        operations.exec { commandLine("docker", "rm", "-v", "-f", "unreal-runner-frontend-build") }
    }
}

val buildFront = tasks.register<BuildFrontendTask>("buildFront")

tasks.processResources {
    dependsOn(buildFront)
}

tasks.register("getLatestChangelogVersion") {
    print(changelog.getLatest().version)
}

val unpackCommitStatusPublisher = tasks.register<Copy>("unpackCommitStatusPublisher") {
    dependsOn("install${teamcity.version}")

    from(zipTree("${teamcity.environments.baseHomeDir}/${teamcity.version}/webapps/ROOT/WEB-INF/plugins/commit-status-publisher.zip")) {
        include("server/commit-status-publisher-*")
        eachFile {
            relativePath = RelativePath(true, relativePath.segments.last())
        }
        includeEmptyDirs = false
    }
    into(layout.projectDirectory.dir("teamcity/dependencies"))
}

// temporary fragile workaround https://youtrack.jetbrains.com/issue/TW-89103
tasks.compileKotlin {
    dependsOn(unpackCommitStatusPublisher)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.serialization.properties)
    implementation(libs.arrow.core)
    implementation(libs.bundles.ktor.client) {
        // rely on the version provided by TeamCity. Otherwise, we get a LinkageError because of "ILoggerFactory"
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation(project(":plugin-sdk-core"))
    implementation(project(":common"))
    provided("org.jetbrains.teamcity.internal:server:${teamcity.version}")

    // temporary fragile workaround https://youtrack.jetbrains.com/issue/TW-89103
    provided(fileTree(layout.projectDirectory.dir("teamcity/dependencies")) {
        include("*.jar")
    })
    testImplementation(fileTree(layout.projectDirectory.dir("teamcity/dependencies")) {
        include("*.jar")
    })

    agent(project(path = ":agent", configuration = "plugin"))

    constraints {
        implementation(libs.constraint.transitive.icu4j) {
            because("previous versions have faulty jar files which cause problems during incremental compilation (which is enabled by default since Kotlin 1.8.20)")
        }
    }

    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testRuntimeOnly(libs.junit.platform.launcher)
}
