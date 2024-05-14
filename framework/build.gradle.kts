plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
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
    val teamcityVersion: String by project

    compileOnly("org.jetbrains.teamcity:agent-api:${teamcityVersion}")
    compileOnly("org.jetbrains.teamcity:common-api:${teamcityVersion}")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("io.arrow-kt:arrow-core:1.2.1")
    implementation("org.apache.commons:commons-configuration2:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    constraints {
        implementation("com.ibm.icu:icu4j") {
            because("previous versions have faulty jar files which cause problems during incremental compilation (which is enabled by default since Kotlin 1.8.20)")
            version { require("3.4.4") }
        }
    }

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.teamcity:tests-support:${teamcityVersion}")
    testImplementation("org.jetbrains.teamcity:agent-api:${teamcityVersion}")
    testImplementation("org.jetbrains.teamcity:common-api:${teamcityVersion}")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
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
