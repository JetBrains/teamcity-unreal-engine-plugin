plugins {
    id("plugin.common")
    id(libs.plugins.teamcity.server.get().pluginId)
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

            // new agent API is used since that version
            minimumBuild = "116751" // 2022.10
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
}

val buildFront: TaskProvider<Task> = tasks.register("buildFront") {
    inputs.dir("./frontend")

    val output = "./src/main/resources/buildServerResources/react"
    outputs.dir(output)

    doLast {
        exec {
            workingDir("./frontend")
            commandLine("docker", "build", "-f", "./build.Dockerfile", "-t", "unreal-runner-frontend-build", ".")
        }
        exec {
            commandLine("docker", "run", "--name", "unreal-runner-frontend-build", "unreal-runner-frontend-build")
        }
        exec {
            commandLine("docker", "cp", "unreal-runner-frontend-build:/app/dist/.", output)
        }
        exec {
            commandLine("docker", "rm", "-v", "-f", "unreal-runner-frontend-build")
        }
    }
}

tasks.processResources {
    dependsOn(buildFront)
}

tasks.register("getLatestChangelogVersion") {
    print(changelog.getLatest().version)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.serialization.properties)
    implementation(libs.arrow.core)
    implementation(project(":framework"))
    implementation(project(":common"))
    provided("org.jetbrains.teamcity.internal:server:${teamcity.version}")
    agent(project(path = ":agent", configuration = "plugin"))

    constraints {
        implementation(libs.constraint.transitive.icu4j) {
            because("previous versions have faulty jar files which cause problems during incremental compilation (which is enabled by default since Kotlin 1.8.20)")
        }
    }

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
