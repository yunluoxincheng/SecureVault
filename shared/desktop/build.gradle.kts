plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                api(project(":shared:common"))
            }
        }
    }
}