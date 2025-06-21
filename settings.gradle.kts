@file:Suppress("UnstableApiUsage")

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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/ksoap2-android-releases/")

        // Repositorios de GitLab con autenticación
        listOf(
            "67926409" to "DEPLOY_TOKEN_EASY_FLOAT",
            "67926057" to "DEPLOY_TOKEN_IMAGE_FLOAT",
            "67926390" to "DEPLOY_TOKEN_ZXING",
            "67926176" to "DEPLOY_TOKEN_DATE_PICKER",
            "68106455" to "DEPLOY_TOKEN_HONEYWELL_SCANNER"
        ).forEach { (projectId, tokenProperty) ->
            maven {
                url = uri("https://gitlab.com/api/v4/projects/$projectId/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = if (providers.gradleProperty("CI_JOB_TOKEN").isPresent) "Job-Token" else "Deploy-Token"
                    value = providers.gradleProperty("CI_JOB_TOKEN").getOrElse(
                        providers.gradleProperty(tokenProperty).getOrElse("")
                    )
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    }
}

rootProject.name = "WarehouseCounter"
include(":app")
 