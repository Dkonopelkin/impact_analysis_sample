pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Impact_analysis_sample"

apply(from = "$rootDir/build_scripts/module_rules.gradle.kts")
apply(from = "$rootDir/build_scripts/settings/app.gradle.kts")
apply(from = "$rootDir/build_scripts/include_modules.gradle.kts")
 