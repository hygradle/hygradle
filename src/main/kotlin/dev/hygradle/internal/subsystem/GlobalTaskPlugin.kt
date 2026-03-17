@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.subsystem

import dev.hygradle.internal.extension.hygradleConfigurations
import dev.hygradle.internal.extension.pluginTaskRegistry
import dev.hygradle.internal.task.DownloadAssets
import dev.hygradle.internal.task.ExtractAssets
import dev.hygradle.internal.task.GenerateSources
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class GlobalTaskPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val settings = project.hygradleSettings()
    val configurations = project.hygradleConfigurations()
    val registry = project.pluginTaskRegistry()
    val hygradleCacheDir = project.gradle.gradleUserHomeDir.resolve("caches/hygradle")

    registry.downloadAssets =
        project.tasks.register<DownloadAssets>("downloadAssets") {
          group = "hygradle/internal"
          version.set(settings.hytale.version)
          patchline.set(settings.hytale.patchline)
          assetBundleCacheDirectory.fileValue(hygradleCacheDir.resolve("bundles"))
        }

    registry.extractAssets =
        project.tasks.register<ExtractAssets>("extractAssets") {
          group = "hygradle/internal"
          assetBundle.from(registry.downloadAssets.map { it.assetBundleCacheDirectory.asFileTree })
          assetCacheDirectory.fileValue(hygradleCacheDir.resolve("assets"))
        }

    registry.generateSources =
        if (settings.hytale.decompile.get()) {
          project.tasks.register<GenerateSources>("generateSources") {
            group = "hygradle/internal"
            description = "Decompile Hytale Server sources for IDE integration."
            serverJar.from(configurations.hytaleClasspath)
            vineflower.from(configurations.vineflowerClasspath)
            version.set(settings.hytale.version)
            outputDirectory.fileValue(hygradleCacheDir.resolve("decompiled"))
          }
        } else null
  }
}
