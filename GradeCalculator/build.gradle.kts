plugins {
    kotlin("jvm") version "2.3.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Tes dépendances pour Excel, PDF et les logs
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")

    // Ajoute aussi ceci pour que Kotlin lui-même fonctionne bien
    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
}