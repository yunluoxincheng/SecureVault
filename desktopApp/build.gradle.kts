plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared:common"))
                implementation(project(":shared:desktop"))
                implementation(project(":composeApp"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.preview)
                implementation(compose.components.resources)
                implementation(compose.desktop.currentOs)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.koin.core)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.securevault.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "SecureVault"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(project.file("icon.ico"))
            }
            macOS {
                iconFile.set(project.file("icon.icns"))
            }
            linux {
                iconFile.set(project.file("icon.png"))
            }
        }
    }
}