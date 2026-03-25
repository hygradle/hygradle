package dev.hygradle.internal

import dev.hygradle.internal.extension.RepositoryExtension
import dev.hygradle.internal.extension.root.RootConfigurations
import dev.hygradle.internal.extension.root.rootConfigurations
import dev.hygradle.internal.service.settings.settingsService
import dev.hygradle.internal.task.DownloadAssets
import dev.hygradle.internal.task.ExtractAssets
import dev.hygradle.internal.task.GenerateSources
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register

class HygradleRootPlugin : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) {
        val configs = extensions.create<RootConfigurations>("hygradle-root-configurations")

        configs.hytaleOnly.configure {
          dependencies.addLater(
              settingsService().hytaleVersion.map {
                dependencyFactory.create("com.hypixel.hytale:Server:$it")
              }
          )
        }

        configs.vineflowerOnly.configure {
          dependencies.addLater(
              settingsService().vineflowerVersion.map {
                dependencyFactory.create("org.vineflower:vineflower:$it")
              }
          )
        }

        registerTasks()

        val repoExt =
            objects.newInstance<RepositoryExtension>(
                project.repositories,
                settingsService().hytalePatchline,
            )

        repoExt.repositories()
      }

  context(project: Project)
  private fun registerTasks() {
    val hygradleSettings = project.settingsService()
    val rootConfigs = project.rootConfigurations()
    val cacheDir = project.gradle.gradleUserHomeDir.resolve("caches/hygradle")

    val downloadAssets =
        project.tasks.register<DownloadAssets>("downloadAssets") {
          group = "hygradle/internal"
          version.set(hygradleSettings.hytaleVersion)
          patchline.set(hygradleSettings.hytalePatchline)
          assetBundleCacheDirectory.fileValue(cacheDir.resolve("bundles"))
        }

    val extractAssets =
        project.tasks.register<ExtractAssets>("extractAssets") {
          group = "hygradle/internal"
          assetBundle.from(downloadAssets.map { it.assetBundleCacheDirectory.asFileTree })
          assetCacheDirectory.fileValue(cacheDir.resolve("assets"))
        }

    rootConfigs.hytaleAssetsRuntimeElements.configure {
      outgoing.artifacts(extractAssets.map { it.assetCacheDirectory.asFileTree }) {
        builtBy(extractAssets)
      }
    }

    // TODO: Remove this shit once the source is shared
    if (hygradleSettings.hytaleDecompile.get()) {
      project.tasks.register<GenerateSources>("generateSources") {
        group = "hygradle/internal"
        description = "Decompile Hytale Server sources for IDE integration."
        serverJar.from(rootConfigs.hytaleClasspath)
        vineflower.from(rootConfigs.vineflowerClasspath)
        version.set(hygradleSettings.hytaleVersion)
        outputDirectory.fileValue(cacheDir.resolve("decompiled"))
      }
    }
  }
}
