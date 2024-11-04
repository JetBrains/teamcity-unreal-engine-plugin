rootProject.name = "teamcity-unreal-engine-plugin"

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
