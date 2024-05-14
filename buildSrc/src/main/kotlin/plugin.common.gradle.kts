import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.diffplug.spotless")
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

project.group = "teamcity-unreal-engine-plugin"
project.version = if (project.findProperty("version") == "unspecified") {
    "SNAPSHOT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYYMMddHHmmss"))
} else {
    project.version
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

spotless {
    kotlin {
        ktlint("0.50.0")
			.setEditorConfigPath("${rootDir}/.editorconfig")
    }
}

tasks {
    compileKotlin {
		compilerOptions {
			allWarningsAsErrors.set(true)
            freeCompilerArgs.add("-Xcontext-receivers")
            jvmTarget.set(JvmTarget.JVM_11)
		}
    }

    compileTestKotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
