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
        // Mapbox requirement
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                // Do not change the username. It must be "mapbox".
                username = "mapbox"
                // Reading token from gradle.properties
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").getOrElse("")
            }
        }
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
