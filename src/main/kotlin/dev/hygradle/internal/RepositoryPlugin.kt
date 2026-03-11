@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal

import dev.hygradle.dsl.hytale.Patchline
import java.net.URI
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginAware

class RepositoryPlugin : Plugin<PluginAware> {
  override fun apply(target: PluginAware) {
    when (target) {
      is Settings -> {
        target.dependencyResolutionManagement.repositories.applyHytaleRepositories()
        target.gradle.plugins.apply(RepositoryPlugin::class.java)
      }
      is Project -> {
        val settingsApplied = target.gradle.plugins.hasPlugin(RepositoryPlugin::class.java)
        // TODO: Replace afterEvaluate if/when repositories get lazy equivalents
        target.afterEvaluate {
          if (!settingsApplied || repositories.isNotEmpty()) {
            repositories.applyHytaleRepositories()
          }
        }
      }
      is Gradle -> {}
      else -> throw GradleException("RepositoryPlugin cannot be applied to $target")
    }
  }

  private fun RepositoryHandler.applyHytaleRepositories() {
    Patchline.entries.forEach {
      maven {
        name = "hytale-${it.name.lowercase()}"
        url = URI.create(it.mavenRepository)
      }
    }
  }
}
