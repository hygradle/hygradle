package dev.hygradle.internal

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.internal.extension.hygradle
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
            project.tasks.register<GeneratePluginManifest>(
                "generate${
          name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                getDefault()
            ) else it.toString()
          }
        }PluginManifest"
            ) {
              group = "hygradle/plugins/${plugin.name}"
              spec.set(plugin.manifest)
            }

        val preparePluginAssets =
            project.tasks.register<PreparePluginAssets>(
                "prepare${
                  name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        getDefault()
                    ) else it.toString()
                  }
                }PluginAssets"
            ) {
              group = "hygradle/plugins/${plugin.name}"
              pluginName.set(plugin.name)
              pluginManifest.set(generateManifest.flatMap { it.manifest })
              pluginResources.from(plugin.sourceSet.map { it.resources })
            }
      }
    }

    val runs = project.hygradle().runs

    runs.all {}
  }
}
