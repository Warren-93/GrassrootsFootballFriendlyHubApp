import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

// Stubs to satisfy IDE sync for AGP 9.0+ built-in Kotlin
tasks.register("prepareKotlinBuildScriptModel") {}
tasks.register("prepareKotlinIdeaImport") {}

android {
    namespace = "com.gffh.mobile.android"
    // Compose Multiplatform 1.10.3's Android dependencies require
    // compileSdk 35+; bumped here to unbreak the build (was pinned at 34).
    compileSdk = 37

    defaultConfig {
        applicationId = "com.gffh.mobile.android"
        minSdk = 24
        targetSdk = 34
        // CI passes -Pgffh.versionCode / -Pgffh.versionName for Play uploads,
        // since every bundle uploaded to Play needs a higher versionCode.
        versionCode = (providers.gradleProperty("gffh.versionCode").orNull ?: "1").toInt()
        versionName = providers.gradleProperty("gffh.versionName").orNull ?: "0.1.0"
    }

    // The upload keystore never lives in the repo: the release workflow
    // decodes it from a GitHub secret and points these variables at it.
    // Without them, release builds come out unsigned.
    val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
    val releaseSigning = if (keystorePath != null) {
        signingConfigs.create("release") {
            storeFile = file(keystorePath)
            storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
        }
    } else null

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = releaseSigning
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
}
