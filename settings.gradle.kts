rootProject.name = "teamcity-unreal-engine-plugin"

dependencyResolutionManagement {
    versionCatalogs {
        create("frameworkLibs")
            .from(files("framework/gradle/libs.versions.toml"))
    }
}

include("agent")
include("server")
include("common")
include("framework")
