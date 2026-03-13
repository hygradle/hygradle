package dev.hygradle.dsl.settings

import dev.hygradle.dsl.hytale.Patchline
import java.io.File
import java.net.URI
import org.gradle.api.artifacts.dsl.RepositoryHandler

fun RepositoryHandler.hytale() {
  val gradleHome =
      File(System.getenv("GRADLE_USER_HOME") ?: "${System.getProperty("user.home")}/.gradle")

  maven {
    name = "hytale-decompiled-cache"
    url = gradleHome.resolve("caches/hygradle/decompiled").toURI()
  }

  Patchline.entries.forEach { patchline ->
    maven {
      name = "hytale-${patchline.name.lowercase()}"
      url = URI.create(patchline.mavenRepository)
    }
  }

  mavenCentral()
}
