package dev.hygradle.internal.subsystem

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.plugin.sourceSets
import dev.hygradle.internal.task.DownloadAssets
import dev.hygradle.internal.task.ExtractAssets
import dev.hygradle.internal.task.plugin.AssembleAssets
import dev.hygradle.internal.task.plugin.GenerateManifest
import dev.hygradle.internal.task.run.PrepareRunDirectory
import dev.hygradle.internal.task.run.RunHytaleServer
import java.util.Locale.getDefault
import org.gradle.api.Plugin as GradlePlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class TaskPlugin : GradlePlugin<Project> {
  override fun apply(project: Project) {
    val hygradle = project.hygradle()
    val sourceSets = project.sourceSets()
    val objects = project.objects
    val providers = project.providers
    val pluginContainer = hygradle.plugins
    val hygradleCacheDir = project.gradle.gradleUserHomeDir.resolve("caches/hygradle")

    val downloadAssetBundle =
        project.tasks.register<DownloadAssets>("downloadAssets") {
          group = "hygradle/internal"
          version.set(hygradle.hytale.version)
          patchline.set(hygradle.hytale.patchline)
          assetBundleCacheDirectory.fileValue(hygradleCacheDir.resolve("bundles"))
        }

    val extractAssets =
        project.tasks.register<ExtractAssets>("extractAssets") {
          group = "hygradle/internal"
          assetBundle.from(downloadAssetBundle.map { it.assetBundleCacheDirectory.asFileTree })
          assetCacheDirectory.fileValue(hygradleCacheDir.resolve("assets"))
        }

    pluginContainer.all {
      val plugin = this

      if (plugin is LatePlugin) {
        val generateManifest =
            project.tasks
                .register<GenerateManifest>("generate${name.capitalize()}Manifest") {
                  group = "hygradle/plugins/${plugin.name}"
                  spec.set(plugin.manifest)
                }
                .also { plugin.generateManifest.set(it) }

        sourceSets.named(plugin.sourceSetName.get()).configure {
          resources.srcDir(generateManifest.flatMap { t -> t.manifestDirectory })
        }

        project.tasks
            .register<AssembleAssets>("assemble${name.capitalize()}Assets") {
              group = "hygradle/plugins/${plugin.name}"
              pluginName.set(plugin.name)
              pluginManifest.set(generateManifest.flatMap { it.manifest })
              pluginResources.from(
                  plugin.sourceSetName.flatMap { sourceSets.named(it) }.map { it.resources }
              )
            }
            .also { plugin.assembleAssets.set(it) }
      }
    }

    val runs = hygradle.runs

    runs.all {
      val run = this

      run.plugins.convention(providers.provider { pluginContainer.names })

      val prepareRunDirectory =
          project.tasks.register<PrepareRunDirectory>("prepare${name.capitalize()}RunDirectory") {
            group = "hygradle/runs/${run.name}"
            runName.set(run.name)
          }

      project.tasks.register<RunHytaleServer>("start${name.capitalize()}Server") {
        group = "hygradle/runs/${run.name}"

        runDirectory.set(prepareRunDirectory.flatMap { it.runDirectory })
        classpathProvider.from(hygradle.hytale.hytaleClasspath)

        assets.from(extractAssets.map { it.assetCacheDirectory.asFileTree })
        hotswapAgent.from(hygradle.hotswapAgent.hotswapAgentClasspath)
        harness.from(hygradle.harness.harnessClasspath)

        classpathProvider.from(
            run.plugins.map { names ->
              objects.fileCollection().apply {
                for (name in names) {
                  require(name in pluginContainer.names) {
                    "Run '${run.name}' references unknown plugin '$name'"
                  }

                  val plugin = pluginContainer.named(name).get()

                  from(
                      plugin.sourceSetName
                          .flatMap { sourceSets.named(it) }
                          .map { it.output.classesDirs }
                  )

                  from(plugin.runtimeClasspathConfiguration)

                  if (plugin is LatePlugin)
                      from(plugin.assembleAssets.flatMap { it.assetDirectory })
                }
              }
            }
        )
      }
    }
  }
}

internal fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
}
