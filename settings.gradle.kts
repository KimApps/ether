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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Ether"
// core
include(":app")
include(":modules:core:network")
include(":modules:core:utils")
include(":modules:core:local-storage")
// shared
include(":modules:shared:user")
// features
include(":modules:features:home")

include(":modules:shared:ui")
include(":modules:shared:signing")
