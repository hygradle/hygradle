plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

rootProject.name = "hygradle"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.gradle.org/gradle/libs-releases/")
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
