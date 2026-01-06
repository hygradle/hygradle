package dev.hygradle.internal

import java.net.URI
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.PluginAware

@Suppress("UnstableApiUsage")
public class RepositoriesPlugin : Plugin<PluginAware> {
  override fun apply(target: PluginAware) {
    when (target) {
      is Project -> target.repositories.apply()
      is Settings -> {
        target.dependencyResolutionManagement.repositories.apply()
        target.gradle.plugins.apply(this::class.java)
      }
      else -> throw GradleException("")
    }
  }
}

public fun RepositoryHandler.apply() {
  val repositories =
      HytalePatchline.entries.map { patchline ->
        maven {
          it.name = "hytale-${patchline.name.lowercase()}"
          it.url = URI.create(patchline.repository)
        }
      }

  removeAll(repositories)
  addAll(0, repositories)
}

public enum class HytalePatchline(public val repository: String) {
  RELEASE("https://maven.hytale.com/release"),
  PRERELEASE("https://maven.hytale.com/pre-release"),
}
