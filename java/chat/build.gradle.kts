plugins {
    alias(libs.plugins.jvm)
    id("application")
}

dependencies {
    implementation("com.joinself:sdk-jvm:1.0.0-SNAPSHOT")
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // Set to the desired Java version
    }
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "com.joinself.ChatKt"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}