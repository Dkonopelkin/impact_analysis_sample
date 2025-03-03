dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../build_scripts/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"

include(":plugins")
