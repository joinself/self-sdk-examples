pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "SelfExamples"
include(":registration")
include(":verification")
include(":chat-qrcode")
include(":chat")
include(":credential")
include(":backup-restore")