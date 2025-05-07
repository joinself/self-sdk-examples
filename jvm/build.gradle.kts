buildscript {
    repositories {
        google()
        mavenCentral()

        maven(url = "https://maven.fabric.io/public")
        maven(url = "https://plugins.gradle.org/m2/")
        maven(url = "https://artifactory.joinself.com/artifactory/libs-release")
    }

    dependencies {

    }
}

allprojects {
    configurations.all {
        resolutionStrategy {
            cacheDynamicVersionsFor(5, TimeUnit.MINUTES)
            cacheChangingModulesFor(0, TimeUnit.SECONDS)
        }
    }
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.layout.buildDirectory)
}