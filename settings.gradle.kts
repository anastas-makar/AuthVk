pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-android/")
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/maven/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://maven.pkg.github.com/anastas-makar/AuthApi")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }

        maven {
            url =
                uri("https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-android/")
        }
        maven {
            url = uri("https://artifactory-external.vkpartner.ru/artifactory/maven/")
        }
        maven {
            url =
                uri("https://artifactory-external.vkpartner.ru/artifactory/vk-id-captcha/android/")
        }
    }
}

rootProject.name = "Auth Vk"
include(":app")
 