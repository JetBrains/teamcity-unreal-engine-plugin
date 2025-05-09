plugins {
    kotlin("jvm")
    alias(pluginSdkCoreLibs.plugins.kotlin.serialization)
}

group = "com.jetbrains.teamcity.plugins.plugin-sdk-core"
version = "0.0.1"

repositories {
    mavenCentral()
    maven {
        url = uri("https://download.jetbrains.com/teamcity-repository")
    }
}

dependencies {
    compileOnly(pluginSdkCoreLibs.teamcity.agent.api)
    compileOnly(pluginSdkCoreLibs.teamcity.common.api)

    implementation(pluginSdkCoreLibs.kotlin.stdlib)
    implementation(pluginSdkCoreLibs.kotlin.serialization.json)
    implementation(pluginSdkCoreLibs.arrow.core)
    implementation(pluginSdkCoreLibs.kotlin.coroutines.core)
    implementation(pluginSdkCoreLibs.commons.configuration)

    constraints {
        implementation(pluginSdkCoreLibs.constraint.transitive.icu4j) {
            because("previous versions have faulty jar files which cause problems during incremental compilation (which is enabled by default since Kotlin 1.8.20)")
        }
    }

    testImplementation(kotlin("test"))

    testImplementation(pluginSdkCoreLibs.teamcity.tests.support)
    testImplementation(pluginSdkCoreLibs.teamcity.agent.api)
    testImplementation(pluginSdkCoreLibs.teamcity.common.api)
    testImplementation(pluginSdkCoreLibs.mockk)
    testImplementation(pluginSdkCoreLibs.junit.jupiter)
    testRuntimeOnly(pluginSdkCoreLibs.junit.platform.launcher)
    testImplementation(pluginSdkCoreLibs.kotlin.coroutines.test)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks {
    compileKotlin {
        compilerOptions {
            allWarningsAsErrors.set(true)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    compileTestKotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
