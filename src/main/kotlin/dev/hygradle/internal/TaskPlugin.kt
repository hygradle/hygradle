package dev.hygradle.internal

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.plugin.sourceSets
import dev.hygradle.tasks.GeneratePluginManifest
import dev.hygradle.tasks.PreparePluginAssets
import java.util.Locale.getDefault
import org.gradle.api.Plugin as GradlePlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class TaskPlugin : GradlePlugin<Project> {
  override fun apply(project: Project) {
    val plugins = project.hygradle().plugins

    plugins.all {
      val plugin = this

      if (plugin is LatePlugin) {
        val generateManifest =
            project.tasks
                .register<GeneratePluginManifest>("generate${name.capitalize()}PluginManifest") {
                  group = "hygradle/plugins/${plugin.name}"
                  spec.set(plugin.manifest)
                }
                .also { plugin.generateManifest.set(generateManifest) }

        val preparePluginAssets =
            project.tasks
                .register<PreparePluginAssets>("prepare${name.capitalize()}PluginAssets") {
                  group = "hygradle/plugins/${plugin.name}"
                  pluginName.set(plugin.name)
                  pluginManifest.set(generateManifest.flatMap { it.manifest })
                  pluginResources.from(
                      plugin.sourceSetName
                          .flatMap { project.sourceSets().named(it) }
                          .map { it.resources }
                  )
                }
                .also { plugin.prepareAssets.set(it) }
      }
    }

    val runs = project.hygradle().runs

    runs.all {}
  }
}

internal fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
}
