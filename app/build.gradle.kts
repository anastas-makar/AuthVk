import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
}

val lp = gradleLocalProperties(rootDir, providers)

android {
    namespace = "pro.progr.authvk"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // если библиотеке реально нужен этот URL во время выполнения:
        buildConfigField("String", "API_BASE_URL", "\"${lp.getProperty("API_BASE_URL")}\"")
    }

    // AAR не подписывается — signingConfigs не нужен

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // Нужно, чтобы publishing смог собрать sourcesJar и взять правильный компонент
    publishing {
        singleVariant("release") {
            withSourcesJar()
            // withJavadocJar() // если захочешь javadoc
        }
    }
}

dependencies {
    // версии Compose синхронизированы под связку Kotlin 1.9.10 + compiler 1.5.3
    implementation("androidx.compose.ui:ui:1.9.5")
    implementation("androidx.compose.ui:ui-tooling:1.9.5")
    implementation("androidx.compose.foundation:foundation:1.9.5")
    implementation("androidx.compose.material:material:1.9.5")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.activity:activity-compose:1.12.0") // под 1.5.x Compose

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    implementation(platform("com.squareup.okhttp3:okhttp-bom:5.3.2"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    implementation("com.google.crypto.tink:tink-android:1.19.0")

    // твоя общая библиотека с интерфейсом/интерсепторами
    implementation("pro.progr:auth-api:0.0.3-alpha")

    // VK ID SDK
    implementation("com.vk.id:vkid:2.5.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "pro.progr"
            artifactId = "auth-vk"
            version = "1.0.0-alpha"

            // ВАЖНО: берём AAR и sourcesJar из компонента release,
            // а не руками по пути в build/outputs
            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/anastas-makar/AuthVk")
            credentials {
                username = (findProperty("gpr.user") ?: System.getenv("GITHUB_USERNAME")).toString()
                password = (findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN")).toString()
            }
        }
    }
}
