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
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            content {
                includeGroup("com.joinself")
            }
        }
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