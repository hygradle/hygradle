package dev.hygradle.dsl.settings

import dev.hygradle.dsl.hytale.Patchline
import java.net.URI
import org.gradle.api.artifacts.dsl.RepositoryHandler

fun RepositoryHandler.hytale() {
  Patchline.entries.forEach { patchline ->
    maven {
      name = "hytale-${patchline.name.lowercase()}"
      url = URI.create(patchline.mavenRepository)
    }
  }
}
