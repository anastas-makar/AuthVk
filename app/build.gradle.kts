import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

val lp = gradleLocalProperties(rootDir)

android {
    namespace = "pro.progr.authvk"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"${lp.getProperty("API_BASE_URL")}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(lp.getProperty("RELEASE_STORE_FILE") ?: "")
            storePassword = lp.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = lp.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = lp.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-tooling:1.5.4")
    implementation("androidx.compose.material:material:1.5.4")
    implementation("androidx.compose.foundation:foundation:1.5.4")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    implementation("com.google.crypto.tink:tink-android:1.13.0")

    implementation("pro.progr:auth-api:0.0.3-alpha")

    implementation("com.vk.id:vkid:2.5.1")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "pro.progr"
            artifactId = "auth-vk"
            version = "0.0.1-alpha"
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