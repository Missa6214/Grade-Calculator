// ============================================================
// build.gradle.kts (racine du projet)
// ============================================================
// Plugins déclarés ici mais appliqués dans le module "app".
// "false" = ne pas appliquer ici, juste déclarer la version.

plugins {
    id("com.android.application")      version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    kotlin("jvm")
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(8)
}