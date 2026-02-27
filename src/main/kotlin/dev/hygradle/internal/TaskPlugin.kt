package dev.hygradle.internal

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.plugin.sourceSets
import dev.hygradle.internal.task.DownloadAssetBundle
import dev.hygradle.internal.task.ExtractAssets
import dev.hygradle.tasks.ExecuteServerRun
import dev.hygradle.tasks.GeneratePluginManifest
import dev.hygradle.tasks.PreparePluginAssets
import dev.hygradle.tasks.PrepareRunDirectory
import java.util.Locale.getDefault
import org.gradle.api.Plugin as GradlePlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class TaskPlugin : GradlePlugin<Project> {
  override fun apply(project: Project) {
    val downloadAssetBundle =
        project.tasks.register<DownloadAssetBundle>("downloadGameAssets") {
          group = "hygradle/internal"
          version.set(project.hygradle().hytale.version)
          patchline.set(project.hygradle().hytale.patchline)
        }

    val extractAssets =
        project.tasks.register<ExtractAssets>("extractAssets") {
          group = "hygradle/internal"
          assetBundle.set(downloadAssetBundle.flatMap { it.assetBundle })
        }

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

    runs.all {
      val run = this

      val prepareRunDirectory =
          project.tasks.register<PrepareRunDirectory>("prepare${name.capitalize()}RunDirectory") {
            group = "hygradle/runs/${run.name}"
            runName.set(run.name)
          }

      val startServer =
          project.tasks.register<ExecuteServerRun>("start${name.capitalize()}Server") {
            group = "hygradle/runs/${run.name}"

            runDirectory.set(prepareRunDirectory.flatMap { it.runDirectory })
            classpathProvider.from(project.hygradle().hytale.hytaleClasspath)
            assets.set(extractAssets.flatMap { it.assets })

            plugins
                .filter { true } // TODO: Add a prop to specify plugins
                .forEach {
                  classpathProvider.from(
                      it.sourceSetName
                          .flatMap { name -> project.sourceSets().named(name) }
                          .map { sourceSet -> sourceSet.output.classesDirs }
                  )

                  classpathProvider.from(it.runtimeClasspathConfiguration)

                  if (it is LatePlugin)
                      classpathProvider.from(it.prepareAssets.flatMap { t -> t.assetDirectory })
                }
          }
    }
  }
}

internal fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
}
