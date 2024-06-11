plugins {
    kotlin("jvm")
    alias(frameworkLibs.plugins.kotlin.serialization)
}

group = "com.jetbrains.teamcity.plugins.framework"
version = "TODO"

repositories {
    mavenCentral()
    maven {
        url = uri("https://download.jetbrains.com/teamcity-repository")
    }
}

dependencies {
    compileOnly(frameworkLibs.teamcity.agent.api)
    compileOnly(frameworkLibs.teamcity.common.api)

    implementation(frameworkLibs.kotlin.stdlib)
    implementation(frameworkLibs.kotlin.serialization.json)
    implementation(frameworkLibs.arrow.core)
    implementation(frameworkLibs.kotlin.coroutines.core)
    implementation(frameworkLibs.commons.configuration)

    constraints {
        implementation(libs.constraint.transitive.icu4j) {
            because("previous versions have faulty jar files which cause problems during incremental compilation (which is enabled by default since Kotlin 1.8.20)")
        }
    }

    testImplementation(kotlin("test"))

    testImplementation(frameworkLibs.teamcity.tests.support)
    testImplementation(frameworkLibs.teamcity.agent.api)
    testImplementation(frameworkLibs.teamcity.common.api)
    testImplementation(frameworkLibs.mockk)
    testImplementation(frameworkLibs.junit.jupiter)
    testRuntimeOnly(frameworkLibs.junit.platform.launcher)
    testImplementation(frameworkLibs.kotlin.coroutines.test)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.AMAZON)
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
