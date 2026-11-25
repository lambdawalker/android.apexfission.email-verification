pluginManagement {
    resolutionStrategy.eachPlugin {
        if (requested.id.id in setOf("com.android.application", "com.android.library")) {
            useModule("com.android.tools.build:gradle:${requested.version}")
        }
    }
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
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://jitpack.io") {
            content { includeGroup("com.github.lambdawalker") }
        }
    }
}

rootProject.name = "android.apexfission.email-verification"
include(":email-verification", ":app")
