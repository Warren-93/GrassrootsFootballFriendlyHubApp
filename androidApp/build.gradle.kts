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
        targetSdk = 36
        // CI passes -Pgffh.versionCode / -Pgffh.versionName for Play uploads,
        // since every bundle uploaded to Play needs a higher versionCode.
        versionCode = (providers.gradleProperty("gffh.versionCode").orNull ?: "1").toInt()
        versionName = providers.gradleProperty("gffh.versionName").orNull ?: "0.1.0"
    }

    buildFeatures {
        buildConfig = true
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

    // Where the app finds the API. Override with -Pgffh.apiBaseUrl=... or the
    // GFFH_API_BASE_URL environment variable (the release workflow passes the
    // repository variable of the same name). Debug builds default to the
    // emulator's alias for the host machine's localhost.
    val apiBaseUrlOverride = providers.gradleProperty("gffh.apiBaseUrl").orNull?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable("GFFH_API_BASE_URL").orNull?.takeIf { it.isNotBlank() }
    fun quoted(value: String) = "\"" + value.trimEnd('/') + "\""

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", quoted(apiBaseUrlOverride ?: "http://10.0.2.2:8090"))
            // Plain http is only for talking to a backend on your own machine.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = false
            signingConfig = releaseSigning
            buildConfigField("String", "API_BASE_URL", quoted(apiBaseUrlOverride ?: "https://gffh-api.onrender.com"))
            manifestPlaceholders["usesCleartextTraffic"] = "false"
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
