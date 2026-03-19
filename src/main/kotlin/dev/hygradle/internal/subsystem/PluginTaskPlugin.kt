@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.subsystem

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.internal.HygradleAttributes
import dev.hygradle.internal.HygradleVariant
import dev.hygradle.internal.extension.globalTaskRegistry
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.extension.pluginTaskRegistry
import dev.hygradle.internal.plugin.PluginImpl
import dev.hygradle.internal.plugin.sourceSets
import dev.hygradle.internal.task.plugin.AssembleAssets
import dev.hygradle.internal.task.plugin.GenerateManifest
import dev.hygradle.internal.task.plugin.rootDirectoryVisitor
import dev.hygradle.internal.util.capitalize
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class PluginTaskPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val hygradle = project.hygradle()
    val globalRegistry = project.gradle.globalTaskRegistry()
    val registry = project.pluginTaskRegistry()
    val sourceSets = project.sourceSets()
    val pluginContainer = hygradle.plugins
    val generateSources = globalRegistry.generateSources
    val objects = project.objects

    val allRuntimeElements =
        project.configurations.consumable("_allPluginsRuntimeElements") {
          attributes {
            attribute(HygradleAttributes.VARIANT_ATTRIBUTE, HygradleVariant.RUNTIME)
            attribute(HygradleAttributes.PLUGIN_BUNDLE_ATTRIBUTE, true)
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objects.named(LibraryElements.CLASSES),
            )
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
          }
        }

    pluginContainer.all {
      val plugin = this
      val configName = name

      if (generateSources != null) {
        project.tasks
            .named(sourceSets.getByName(plugin.sourceSetName.get()).compileJavaTaskName)
            .configure { dependsOn(generateSources) }
      }

      // Wire into aggregate consumable
      val pluginImpl = plugin as PluginImpl
      allRuntimeElements.configure {
        extendsFrom(pluginImpl.runtimeOnlyConfiguration)
        extendsFrom(pluginImpl.pluginConfiguration)
      }

      // Add classes directory artifacts to the aggregate
      allRuntimeElements.configure {
        val sourceSet = sourceSets.getByName(plugin.sourceSetName.get())
        sourceSet.output.classesDirs.files.forEach { classesDir ->
          outgoing.artifact(classesDir) { builtBy(sourceSet.output) }
        }
      }

      if (plugin is LatePlugin) {
        plugin.manifest.includesAssetPack.convention(
            plugin.sourceSetName
                .flatMap { sourceSets.named(it) }
                .map { sourceSet ->
                  var found = false
                  sourceSet.resources.asFileTree.visit(
                      rootDirectoryVisitor(AssembleAssets.ASSET_DIRECTORY_NAMES) {
                        found = true
                        stopVisiting()
                      }
                  )
                  found
                }
        )

        val runtimeOnly = project.configurations.named("${configName}RuntimeOnly")
        val pluginConfig = project.configurations.named("${configName}Plugin")
        val optionalPluginConfig = project.configurations.named("${configName}OptionalPlugin")

        val depManifestsConfig =
            project.configurations.resolvable("_${configName}DependencyManifests") {
              extendsFrom(runtimeOnly)
              extendsFrom(pluginConfig)

              attributes {
                attribute(HygradleAttributes.VARIANT_ATTRIBUTE, HygradleVariant.RUNTIME)
                attribute(ARTIFACT_TYPE_ATTRIBUTE, HygradleAttributes.PLUGIN_MANIFEST_ARTIFACT_TYPE)
              }
            }

        val optionalDepManifestsConfig =
            project.configurations.resolvable("_${configName}OptionalDependencyManifests") {
              extendsFrom(optionalPluginConfig)

              attributes {
                attribute(HygradleAttributes.VARIANT_ATTRIBUTE, HygradleVariant.RUNTIME)
                attribute(ARTIFACT_TYPE_ATTRIBUTE, HygradleAttributes.PLUGIN_MANIFEST_ARTIFACT_TYPE)
              }
            }

        val generateManifest =
            project.tasks.register<GenerateManifest>("generate${configName.capitalize()}Manifest") {
              group = "hygradle/plugins/$configName"
              manifest.set(plugin.manifest)

              dependencyManifests.from(
                  depManifestsConfig.map { config ->
                    val directPaths =
                        (project.configurations.getByName("${configName}RuntimeOnly").dependencies +
                                project.configurations
                                    .getByName("${configName}Plugin")
                                    .dependencies)
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

              optionalDependencyManifests.from(
                  optionalDepManifestsConfig.map { config ->
                    val directPaths =
                        project.configurations
                            .getByName("${configName}OptionalPlugin")
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

        registry.manifestTasks[configName] = generateManifest

        // Publish manifest as a separate consumable configuration for cross-project consumers
        project.configurations.consumable("${configName}ManifestElements") {
          attributes {
            attribute(HygradleAttributes.VARIANT_ATTRIBUTE, HygradleVariant.RUNTIME)
            attribute(HygradleAttributes.PLUGIN_NAME_ATTRIBUTE, configName)
            attribute(ARTIFACT_TYPE_ATTRIBUTE, HygradleAttributes.PLUGIN_MANIFEST_ARTIFACT_TYPE)
          }

          outgoing.artifact(generateManifest.flatMap { it.manifestFile }) {
            builtBy(generateManifest)
          }
        }

        sourceSets.named(plugin.sourceSetName.get()).configure {
          resources.srcDir(generateManifest.flatMap { t -> t.manifestDirectory })
        }

        val assembleAssets =
            project.tasks.register<AssembleAssets>("assemble${name.capitalize()}Assets") {
              group = "hygradle/plugins/${plugin.name}"
              pluginName.set(plugin.name)
              pluginManifest.set(generateManifest.flatMap { it.manifestFile })
              pluginResources.from(
                  plugin.sourceSetName.flatMap { sourceSets.named(it) }.map { it.resources }
              )
            }

        registry.assetTasks[configName] = assembleAssets

        project.configurations.named("${name}RuntimeElements").configure {
          outgoing.artifact(assembleAssets.flatMap { it.assetDirectory }) {
            builtBy(assembleAssets)
          }
        }

        // Also add asset directory to the aggregate
        allRuntimeElements.configure {
          outgoing.artifact(assembleAssets.flatMap { it.assetDirectory }) {
            builtBy(assembleAssets)
          }
        }
      }
    }
  }
}
