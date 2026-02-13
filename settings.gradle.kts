pluginManagement {
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

rootProject.name = "RouteRecorder"
include(":app")
include(":core:common")
include(":core:data")
include(":core:domain")
include(":feature:tracking")
include(":feature:history")
include(":feature:video")
include(":service:tracking")
include(":service:video-render")
