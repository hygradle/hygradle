@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.subsystem

import dev.hygradle.internal.attributes.Category as HygradleCategory
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.extension.pluginTaskRegistry
import dev.hygradle.internal.plugin.LatePluginImpl
import dev.hygradle.internal.plugin.PluginImpl
import dev.hygradle.internal.plugin.sourceSets
import dev.hygradle.internal.service.settings.settingsService
import dev.hygradle.internal.task.plugin.AssembleAssets
import dev.hygradle.internal.task.plugin.GenerateManifest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class PluginTaskPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val hygradle = project.hygradle()
    val sourceSets = project.sourceSets()
    val pluginContainer = hygradle.plugins

    pluginContainer.all {
      configurePlugin(this as PluginImpl, project)

      // TODO: Rip out when server source is shared
      if (project.settingsService().hytaleDecompile.get()) {
        project.tasks
            .named(sourceSets.getByName(this.sourceSetName.get()).compileJavaTaskName)
            .configure { dependsOn(":generateSources") }
      }

      if (this is LatePluginImpl) {
        configureLatePlugin(this, project)

        // AssetPack heuristics
        //        plugin.manifest.includesAssetPack.convention(
        //            plugin.sourceSetName
        //                .flatMap { sourceSets.named(it) }
        //                .map { sourceSet ->
        //                  var found = false
        //
        //                  sourceSet.resources.asFileTree.visit(
        //                      rootDirectoryVisitor(AssembleAssets.ASSET_DIRECTORY_NAMES) {
        //                        found = true
        //                        stopVisiting()
        //                      }
        //                  )
        //
        //                  found
        //                }
        //        )

      }
    }
  }

  private fun configurePlugin(plugin: PluginImpl, project: Project) {}

  private fun configureLatePlugin(plugin: LatePluginImpl, project: Project) {
    val generateManifest =
        project.tasks.register<GenerateManifest>("generate${plugin.taskSlug}Manifest") {
          group = plugin.taskGroup

          manifest.set(plugin.manifest)

          dependencyManifests.from(
              plugin.runtimeClasspath.map {
                it.incoming
                    .artifactView {
                      attributes.attribute(
                          Category.CATEGORY_ATTRIBUTE,
                          project.objects.named(
                              HygradleCategory.PLUGIN_MANIFEST,
                          ),
                      )
                      lenient(true)
                    }
                    .files
              }
          )

          optionalDependencyManifests.from(
              plugin.compileClasspath.map {
                it.incoming
                    .artifactView {
                      attributes.attribute(
                          Category.CATEGORY_ATTRIBUTE,
                          project.objects.named(
                              HygradleCategory.PLUGIN_MANIFEST,
                          ),
                      )
                      lenient(true)
                    }
                    .files
              }
          )
        }

    project.pluginTaskRegistry().manifestTasks[plugin.name] = generateManifest

    plugin.runtimeElements.configure {
      outgoing.variants.register("manifest") {
        attributes.attribute(
            Category.CATEGORY_ATTRIBUTE,
            project.objects.named(HygradleCategory.PLUGIN_MANIFEST),
        )
        artifact(generateManifest.flatMap { it.manifestFile }) { builtBy(generateManifest) }
      }
    }

    val assembleAssets =
        project.tasks.register<AssembleAssets>("assemble${plugin.taskSlug}Assets") {
          group = plugin.taskGroup
          pluginName.set(plugin.name)
          pluginManifest.set(generateManifest.flatMap { it.manifestFile })

          pluginResources.from(
              plugin.sourceSetName.flatMap { project.sourceSets().named(it) }.map { it.resources }
          )
        }

    project.pluginTaskRegistry().assetTasks[plugin.name] = assembleAssets

    plugin.runtimeElements.configure {
      outgoing.variants.register("assets") {
        attributes.attribute(
            Category.CATEGORY_ATTRIBUTE,
            project.objects.named(HygradleCategory.PLUGIN_ASSETS),
        )
        artifact(assembleAssets.flatMap { it.assetDirectory }) { builtBy(assembleAssets) }
      }
    }

    // We don't know the sourceSet names until they're finalized. A necessary evil :(
    // TODO: Fix this shit if source sets ever get a proper lazy API
    project.afterEvaluate {
      project.sourceSets().named(plugin.sourceSetName.get()).configure {
        resources.srcDir(generateManifest)
      }
    }
  }
}
