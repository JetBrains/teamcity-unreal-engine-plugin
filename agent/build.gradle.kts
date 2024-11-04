plugins {
    id("plugin.common")
    id(libs.plugins.teamcity.agent.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
}

teamcity {
    agent {
        descriptor {
            pluginDeployment {
                useSeparateClassloader = true
            }
        }

        archiveName = "${project.parent?.name}-${project.name}"
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.serialization.properties)
    implementation(libs.arrow.core)
    implementation(libs.kotlin.coroutines.core)
    implementation(project(":plugin-sdk-core"))
    implementation(project(":common"))

    provided("org.jetbrains.teamcity.internal:agent:${teamcity.version}")

    constraints {
        implementation(libs.constraint.transitive.icu4j) {
            because("previous versions have faulty jar files which cause problems during incremental compilation (which is enabled by default since Kotlin 1.8.20)")
        }
    }

    testImplementation(kotlin("test"))
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
