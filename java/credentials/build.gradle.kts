plugins {
    alias(libs.plugins.jvm)
    id("application")
}

dependencies {
    implementation("com.joinself:sdk-jvm:1.0.1")
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "com.joinself.MainKt"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}