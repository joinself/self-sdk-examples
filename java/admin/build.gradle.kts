plugins {
    alias(libs.plugins.jvm)
    id("application")
    id("com.squareup.wire") version "5.5.0"
}

dependencies {
    implementation(libs.self.jvm)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization)

    implementation("com.google.zxing:core:3.5.4")
    implementation("com.squareup.wire:wire-runtime:5.5.0")
}

kotlin {
    jvmToolchain(17)
}
wire {
    kotlin {
        sourcePath {
            srcDir("src/proto")
        }
    }
}
application {
    mainClass = "com.joinself.AdminApp"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}