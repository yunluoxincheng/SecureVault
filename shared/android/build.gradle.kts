plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.securevault.shared.android"
        compileSdk = 36
        minSdk = 29
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val androidMain by getting {
            dependencies {
                api(project(":shared:common"))
            }
        }
    }
}