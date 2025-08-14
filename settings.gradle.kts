rootProject.name = "teamcity-unreal-engine-plugin"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("pluginSdkCoreLibs")
            .from(files("plugin-sdk-core/gradle/libs.versions.toml"))
    }
}

include("agent")
include("server")
include("common")
include("plugin-sdk-core")
