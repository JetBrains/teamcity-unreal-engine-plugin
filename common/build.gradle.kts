plugins {
    id("plugin.common")
    id(libs.plugins.teamcity.common.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.serialization.properties)
    implementation(project(":plugin-sdk-core"))

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
    testRuntimeOnly(libs.junit.platform.launcher)
}
