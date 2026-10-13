rootProject.name = "gffh-mobile"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // compose-webview-multiplatform's desktop artifact depends on JOGL,
        // which is only published here. Release lint resolves it, so
        // bundleRelease fails without this repository.
        maven("https://jogamp.org/deployment/maven") {
            content { includeGroupByRegex("org\\.jogamp.*") }
        }
    }
}

include(":shared")
include(":androidApp")
