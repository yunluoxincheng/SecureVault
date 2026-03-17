pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SecureVault"

include(":shared")
include(":shared:common")
include(":shared:android")
include(":shared:ios")
include(":shared:desktop")
include(":composeApp")
include(":androidApp")
include(":desktopApp")