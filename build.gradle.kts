plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    application
}

group = "io.github.nottaras.briefing"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.github.nottaras.briefing.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Force patched Netty over Ktor's transitive pull (CVE-2025-55163, CVE-2025-58057, CVE-2026-33871)
    implementation(enforcedPlatform(libs.netty.bom))

    // HTTP client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // OAuth2 callback server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // CLI
    implementation(libs.clikt)

    // Terminal output
    implementation(libs.mordant)

    // Config (TOML)
    implementation(libs.hoplite.toml)

    // SQLite (history cache — phase 2)
    implementation(libs.sqlite.jdbc)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)

    // Logging
    implementation(libs.logback.classic)

    // Tests
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("briefing")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}
