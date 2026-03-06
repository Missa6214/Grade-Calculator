// ============================================================
// settings.gradle.kts — Paramètres globaux du projet
// ============================================================

pluginManagement {
    repositories {
        google()           // Dépôt Google (Android, Compose...)
        mavenCentral()     // Dépôt central Maven
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.3.10"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GradeCalculator"   // Nom du projet
include(":app")                         // Le projet contient un module "app"
