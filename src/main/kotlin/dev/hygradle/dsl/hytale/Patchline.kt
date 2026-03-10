package dev.hygradle.dsl.hytale

enum class Patchline(val mavenRepository: String, val cdnSlug: String) {
  RELEASE("https://maven.hytale.com/release", "release"),
  PRERELEASE("https://maven.hytale.com/pre-release", "pre-release"),
}
