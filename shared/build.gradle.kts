import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val enableIos = (findProperty("gffh.enableIos") as String?).toBoolean()

kotlin {
    android {
        namespace = "com.gffh.mobile"
        // Compose Multiplatform 1.10.3's Android dependencies require
        // compileSdk 35+; bumped here to unbreak the build (was pinned at 34).
        compileSdk = 37
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        // Runs commonTest (Navigator, ApiResult, Validators, ...) on a plain
        // JVM against Android's real actuals - no emulator/device needed, so
        // it runs on this Windows dev machine and in the existing Android CI
        // job with no new infrastructure.
        withHostTestBuilder {}.configure {}
    }

    // FlowRow (feature/arrange, feature/availability, feature/discover,
    // feature/onboarding) is commonMain code compiled by every target,
    // Android and iOS alike - opt in once here instead of per-target.
    sourceSets.all {
        languageSettings.optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
    }

    // Kotlin/Native provisioning for iOS is deliberately opt-in - see
    // gradle.properties. Enable with -Pgffh.enableIos=true on a machine with
    // Xcode where the iosApp module can actually be built and run.
    //
    // No iosX64 (Intel simulator): Compose Multiplatform 1.11.1 doesn't
    // publish artifacts for it ("Unresolved platforms: [iosX64]"), and every
    // Mac capable of running current Xcode is Apple Silicon anyway.
    if (enableIos) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { target ->
            target.binaries.framework {
                baseName = "shared"
                isStatic = true
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // api, not implementation: androidApp's MainActivity calls
                // setContent { App() } directly and needs these on its
                // classpath too, transitively through :shared.
                //
                // Using the compose.* accessors (not hardcoded coordinates)
                // so these always resolve to whatever version the
                // org.jetbrains.compose plugin itself is pinned to (1.11.1) -
                // a hardcoded "1.7.3" here silently drifted behind the
                // plugin version and produced a "runtime dependencies'
                // versions don't match with plugin version" mismatch that
                // crashed the Kotlin/Native compiler's IR lowering pass with
                // a StackOverflowError when linking the iOS framework.
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material3)
                api(compose.materialIconsExtended)
                api(compose.ui)
                api(compose.components.resources)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                // Pinned to match what Compose Multiplatform's material3-uikitsimarm64
                // already forces transitively on iOS (confirmed via
                // `./gradlew :shared:dependencyInsight --dependency kotlinx-datetime`) -
                // leaving this at 0.6.1 let Android and iOS silently resolve to two
                // different actual Clock types (0.6.1's own interface vs 0.7.1's
                // typealias to kotlin.time.Clock), which is what broke Clock.System.
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-client-logging:2.3.12")
                implementation("io.ktor:ktor-client-auth:2.3.12")

                implementation("com.russhwolf:multiplatform-settings:1.2.0")
                implementation("com.russhwolf:multiplatform-settings-coroutines:1.2.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.12")
                implementation("androidx.activity:activity-compose:1.9.2")
                implementation("androidx.appcompat:appcompat:1.7.0")
            }
        }
        if (enableIos) {
            val iosArm64Main by getting
            val iosSimulatorArm64Main by getting
            val iosMain by creating {
                dependsOn(commonMain)
                iosArm64Main.dependsOn(this)
                iosSimulatorArm64Main.dependsOn(this)
                dependencies {
                    implementation("io.ktor:ktor-client-darwin:2.3.12")
                }
            }
        }
    }
}
