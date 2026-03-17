import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.detekt)
}

allprojects {
    group = "com.securevault"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        config.setFrom(rootProject.file("detekt.yml"))
        buildUponDefaultConfig = true
        allRules = false
    }

    tasks.withType<Detekt>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}