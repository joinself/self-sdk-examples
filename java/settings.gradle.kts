rootProject.name = "JVM_Examples"

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
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }

        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
        mavenCentral()
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://maven.microblink.com") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://maven.scijava.org/content/repositories/public/") }
        maven { url = uri("https://jitpack.io")  }
    }
}

include("chat")
include("discovery")
include("agreement")
include("credentials")
include("custom-credentials")
include("self-demo")
