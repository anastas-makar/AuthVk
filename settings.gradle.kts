pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://nexus-external.vkteam.ru/repository/maven/")
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

        maven {
            url = uri("https://maven.pkg.github.com/anastas-makar/PersonalCrypto")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(System.getenv("GITHUB_USERNAME") ?: "").get()
                password = providers.gradleProperty("gpr.token")
                    .orElse(System.getenv("GITHUB_TOKEN") ?: "").get()
            }
        }

        // VK ID репозитории (для зависимостей VK)
        maven(url = "https://nexus-external.vkteam.ru/repository/maven/")
    }
}

rootProject.name = "Auth Vk"
include(":app")

