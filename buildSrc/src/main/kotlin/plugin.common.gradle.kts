
import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("com.diffplug.spotless")
    id("io.github.rodm.teamcity-base")
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

val libs = the<LibrariesForLibs>()

teamcity {
    version = libs.versions.teamcity.get()
    validateBeanDefinition = com.github.rodm.teamcity.ValidationMode.FAIL
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

spotless {
    kotlin {
        ktlint(libs.versions.ktlint.get())
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
