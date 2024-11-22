pluginManagement {
    listOf(repositories, dependencyResolutionManagement.repositories).forEach {
        it.apply {
            google()
            mavenCentral()
            gradlePluginPortal()
            maven {
                url = uri("https://androidx.dev/snapshots/builds/14637376/artifacts/repository")
            }

            val kotlin_repo_url: String? by settings
            val kotlin_version: String? by settings
            val repoUrl = kotlin_repo_url
            val kotlinVersion = kotlin_version
            if (!repoUrl.isNullOrEmpty() && !kotlinVersion.isNullOrEmpty()) {
                exclusiveContent {
                    forRepository {
                        maven(repoUrl)
                    }
                    filter {
                        includeVersionByRegex("org\\.jetbrains\\.kotlin.*", ".*", kotlinVersion)
                    }
                }
            }
        }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("com.google.cloud.tools.appengine")) {
                useModule("com.google.cloud.tools:appengine-gradle-plugin:${requested.version}")
            }
        }
    }
}

rootProject.name = "PeopleInSpace"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")
include(":wearApp")
include(":compose-desktop")
include(":compose-web")
include(":common")
include(":backend")
include(":mcp-server")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val kotlin_version: String? = settings.providers.gradleProperty("kotlin_version").orNull
            if (kotlin_version != null) {
                version("kotlin", kotlin_version)
            }
        }
    }
}