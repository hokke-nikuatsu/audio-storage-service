pluginManagement {
    includeBuild("build-logic")
}

plugins {
    // toolchain の JDK が手元になければ、Gradle に取ってこさせる。settings の plugins では version catalog を使えないので、バージョンはここに書く
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "audio-storage-service"

include("domain", "application", "adapter", "web", "bootstrap")
