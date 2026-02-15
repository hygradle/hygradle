plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")

rootProject.name = "hygradle"
