package dev.hygradle.dsl.hytale

enum class Patchline(val repository: String) {
  RELEASE("https://maven.hytale.com/release"),
  PRERELEASE("https://maven.hytale.com/pre-release"),
}
