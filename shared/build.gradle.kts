plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()
}

tasks.named("compileKotlinJvm") {
    enabled = false
}