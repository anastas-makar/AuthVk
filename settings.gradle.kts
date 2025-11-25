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

        // GitHub Packages: чтение артефактов (pro.progr:auth-api и т.п.)
        maven {
            url = uri("https://maven.pkg.github.com/anastas-makar/AuthApi")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(System.getenv("GITHUB_USERNAME") ?: "").get()
                password = providers.gradleProperty("gpr.token")
                    .orElse(System.getenv("GITHUB_TOKEN") ?: "").get()
            }
        }

        // VK ID репозитории (для зависимостей VK)
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vkid-sdk-android/")
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/maven/")
        maven(url = "https://artifactory-external.vkpartner.ru/artifactory/vk-id-captcha/android/")
    }
}

rootProject.name = "Auth Vk"
include(":app")
