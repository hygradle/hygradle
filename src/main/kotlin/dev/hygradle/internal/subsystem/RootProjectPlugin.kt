@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.subsystem

import dev.hygradle.dsl.settings.HygradleSettings
import dev.hygradle.internal.extension.GlobalTaskRegistry
import dev.hygradle.internal.service.hytale.HytaleAccount
import dev.hygradle.internal.task.DownloadAssets
import dev.hygradle.internal.task.ExtractAssets
import dev.hygradle.internal.task.GenerateSources
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class RootProjectPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val settings =
        project.gradle.extensions.findByType(HygradleSettings::class.java)
            ?: throw GradleException("HygradleSettings not found on gradle.extensions")

    val hygradleCacheDir = project.gradle.gradleUserHomeDir.resolve("caches/hygradle")
    val registry = GlobalTaskRegistry()

    project.gradle.extensions.add(GlobalTaskRegistry::class.java, "globalTaskRegistry", registry)

    project.gradle.sharedServices.registerIfAbsent(
        "hytale-account",
        HytaleAccount::class.java,
    ) {
      parameters {
        oauthBaseUrl.convention(
            project.providers
                .gradleProperty(HytaleAccount.OAUTH_BASE_PROPERTY)
                .orElse(HytaleAccount.OAUTH_BASE)
        )

        accountBaseUrl.convention(
            project.providers
                .gradleProperty(HytaleAccount.ACCOUNT_BASE_PROPERTY)
                .orElse(HytaleAccount.ACCOUNT_BASE)
        )

        sessionBaseUrl.convention(
            project.providers
                .gradleProperty(HytaleAccount.SESSION_BASE_PROPERTY)
                .orElse(HytaleAccount.SESSION_BASE)
        )

        tokenFile.set(
            project.layout.buildDirectory.dir("hygradle/auth").map { it.file("auth.json") }
        )
      }
    }

    // Use internal-prefixed names to avoid collisions if dev.hygradle is also applied to root
    val hytaleOnly = project.configurations.dependencyScope("_hygradleGlobalHytaleOnly")
    val hytaleClasspath =
        project.configurations.resolvable("_hygradleGlobalHytaleClasspath") {
          extendsFrom(hytaleOnly)
        }

    project.dependencies.addProvider(
        hytaleOnly.name,
        settings.hytale.version.map {
          project.dependencies.create("com.hypixel.hytale:Server:$it")
        },
    )

    val vineflowerOnly = project.configurations.dependencyScope("_hygradleGlobalVineflowerOnly")

    val vineflowerClasspath =
        project.configurations.resolvable("_hygradleGlobalVineflowerClasspath") {
          extendsFrom(vineflowerOnly)
        }

    if (settings.hytale.decompile.get()) {
      project.dependencies.add(vineflowerOnly.name, "org.vineflower:vineflower:1.11.1")
    }

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
            serverJar.from(hytaleClasspath)
            vineflower.from(vineflowerClasspath)
            version.set(settings.hytale.version)
            outputDirectory.fileValue(hygradleCacheDir.resolve("decompiled"))
          }
        } else null
  }
}
