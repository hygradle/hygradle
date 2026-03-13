@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.subsystem

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.internal.HygradleAttributes
import dev.hygradle.internal.HygradleVariant
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.extension.hygradleConfigurations
import dev.hygradle.internal.plugin.sourceSets
import dev.hygradle.internal.task.DownloadAssets
import dev.hygradle.internal.task.ExtractAssets
import dev.hygradle.internal.task.GenerateSources
import dev.hygradle.internal.task.plugin.AssembleAssets
import dev.hygradle.internal.task.plugin.GenerateManifest
import dev.hygradle.internal.task.run.PrepareRunDirectory
import dev.hygradle.internal.task.run.RunHytaleServer
import java.util.Locale.getDefault
import org.gradle.api.Plugin as GradlePlugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.kotlin.dsl.register

class TaskPlugin : GradlePlugin<Project> {
  override fun apply(project: Project) {
    val hygradle = project.hygradle()
    val settings = project.hygradleSettings()
    val configurations = project.hygradleConfigurations()
    val sourceSets = project.sourceSets()
    val objects = project.objects
    val providers = project.providers
    val pluginContainer = hygradle.plugins
    val hygradleCacheDir = project.gradle.gradleUserHomeDir.resolve("caches/hygradle")

    val downloadAssetBundle =
        project.tasks.register<DownloadAssets>("downloadAssets") {
          group = "hygradle/internal"
          version.set(settings.hytale.version)
          patchline.set(settings.hytale.patchline)
          assetBundleCacheDirectory.fileValue(hygradleCacheDir.resolve("bundles"))
        }

    val extractAssets =
        project.tasks.register<ExtractAssets>("extractAssets") {
          group = "hygradle/internal"
          assetBundle.from(downloadAssetBundle.map { it.assetBundleCacheDirectory.asFileTree })
          assetCacheDirectory.fileValue(hygradleCacheDir.resolve("assets"))
        }

    val generateSources =
        if (settings.hytale.decompile.get()) {
          project.tasks.register<GenerateSources>("generateSources") {
            group = "hygradle"
            description = "Decompile Hytale Server sources for IDE integration."
            serverJar.from(configurations.hytaleClasspath)
            vineflower.from(configurations.vineflowerClasspath)
            version.set(settings.hytale.version)
            outputDirectory.fileValue(hygradleCacheDir.resolve("decompiled"))
          }
        } else null

    pluginContainer.all {
      val plugin = this
      val configName = name

      if (generateSources != null) {
        project.tasks
            .named(sourceSets.getByName(plugin.sourceSetName.get()).compileJavaTaskName)
            .configure { dependsOn(generateSources) }
      }

      if (plugin is LatePlugin) {
        val runtimeOnly = project.configurations.named("${configName}RuntimeOnly")

        val depManifestsConfig =
            project.configurations.resolvable("_${configName}DependencyManifests") {
              extendsFrom(runtimeOnly)

              attributes {
                attribute(HygradleAttributes.VARIANT_ATTRIBUTE, HygradleVariant.RUNTIME)
                attribute(ARTIFACT_TYPE_ATTRIBUTE, HygradleAttributes.PLUGIN_MANIFEST_ARTIFACT_TYPE)
              }
            }

        val generateManifest =
            project.tasks
                .register<GenerateManifest>("generate${configName.capitalize()}Manifest") {
                  group = "hygradle/plugins/$configName"
                  spec.set(plugin.manifest)

                  dependencyManifests.from(
                      depManifestsConfig.map { config ->
                        val directPaths =
                            project.configurations
                                .getByName("${configName}RuntimeOnly")
                                .dependencies
                                .filterIsInstance<ProjectDependency>()
                                .mapTo(mutableSetOf()) { it.path }

                        config.incoming
                            .artifactView {
                              componentFilter { id ->
                                id is ProjectComponentIdentifier && id.projectPath in directPaths
                              }
                              lenient(true)
                            }
                            .files
                      }
                  )
                }
                .also { plugin.generateManifest.set(it) }

        // Publish manifest as a separate consumable configuration for cross-project consumers
        project.configurations.consumable("${configName}ManifestElements") {
          attributes {
            attribute(HygradleAttributes.VARIANT_ATTRIBUTE, HygradleVariant.RUNTIME)
            attribute(HygradleAttributes.PLUGIN_NAME_ATTRIBUTE, configName)
            attribute(ARTIFACT_TYPE_ATTRIBUTE, HygradleAttributes.PLUGIN_MANIFEST_ARTIFACT_TYPE)
          }

          outgoing.artifact(generateManifest.flatMap { it.manifest }) { builtBy(generateManifest) }
        }

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
            .also { assembleAssets ->
              plugin.assembleAssets.set(assembleAssets)

              // Publish asset directory for cross-project consumers
              project.configurations.named("${name}RuntimeElements").configure {
                outgoing.artifact(assembleAssets.flatMap { it.assetDirectory }) {
                  builtBy(assembleAssets)
                }
              }
            }
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

        if (generateSources != null) {
          dependsOn(generateSources)
        }

        runDirectory.set(prepareRunDirectory.flatMap { it.runDirectory })
        classpathProvider.from(configurations.hytaleClasspath)

        assets.from(extractAssets.map { it.assetCacheDirectory.asFileTree })
        hotswapAgent.from(configurations.hotswapAgentClasspath)
        harness.from(configurations.harnessClasspath)

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

                  from(project.configurations.named("${name}RuntimeClasspath"))

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
