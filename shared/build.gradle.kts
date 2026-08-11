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
        compileSdk = 34
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
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
    if (enableIos) {
        listOf(
            iosX64(),
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
                api("org.jetbrains.compose.runtime:runtime:1.7.3")
                api("org.jetbrains.compose.foundation:foundation:1.7.3")
                api("org.jetbrains.compose.material3:material3:1.7.3")
                api("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                api("org.jetbrains.compose.ui:ui:1.7.3")
                api("org.jetbrains.compose.components:components-resources:1.7.3")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

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
            val iosX64Main by getting
            val iosArm64Main by getting
            val iosSimulatorArm64Main by getting
            val iosMain by creating {
                dependsOn(commonMain)
                iosX64Main.dependsOn(this)
                iosArm64Main.dependsOn(this)
                iosSimulatorArm64Main.dependsOn(this)
                dependencies {
                    implementation("io.ktor:ktor-client-darwin:2.3.12")
                }
            }
        }
    }
}
