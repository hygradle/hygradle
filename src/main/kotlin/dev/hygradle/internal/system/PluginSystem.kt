@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.system

import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.extension.hygradleConfigurations
import dev.hygradle.internal.plugin.LatePluginImpl
import dev.hygradle.internal.plugin.PluginImpl
import dev.hygradle.internal.plugin.sourceSets
import dev.hygradle.internal.service.settings.settingsService
import dev.hygradle.internal.task.plugin.AssembleAssets
import dev.hygradle.internal.task.plugin.GenerateManifest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources

class PluginSystem : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) {
        hygradle().plugins.all {
          configurePlugin(this as PluginImpl)

          // TODO: Rip out when server source is shared
          if (settingsService().hytaleDecompile.get()) {
            tasks
                .named(sourceSets().getByName(this.sourceSetName.get()).compileJavaTaskName)
                .configure { dependsOn(":generateSources") }
          }

          if (this is LatePluginImpl) configureLatePlugin(this)
        }
      }

  context(project: Project)
  private fun configurePlugin(plugin: PluginImpl) {
    project.configurations
        .named(
            project.sourceSets().getByName(plugin.sourceSetName.get()).compileOnlyConfigurationName
        )
        .configure { extendsFrom(project.hygradleConfigurations().hytaleOnly) }
  }

  context(project: Project)
  private fun configureLatePlugin(plugin: LatePluginImpl) {
    plugin.configureConventions()

    plugin.generateManifest =
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
                              dev.hygradle.internal.attributes.Category.PLUGIN_MANIFEST,
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
                              dev.hygradle.internal.attributes.Category.PLUGIN_MANIFEST,
                          ),
                      )
                      lenient(true)
                    }
                    .files
              }
          )
        }

    for (elements in listOf(plugin.runtimeElements, plugin.compileElements)) {
      elements.configure {
        outgoing.variants.register("manifest") {
          attributes.attribute(
              Category.CATEGORY_ATTRIBUTE,
              project.objects.named(dev.hygradle.internal.attributes.Category.PLUGIN_MANIFEST),
          )
          artifact(plugin.generateManifest.flatMap { it.manifestFile }) {
            builtBy(plugin.generateManifest)
          }
        }
      }
    }

    plugin.assembleAssets =
        project.tasks.register<AssembleAssets>("assemble${plugin.taskSlug}Assets") {
          group = plugin.taskGroup
          pluginName.set(plugin.name)
          pluginManifest.set(plugin.generateManifest.flatMap { it.manifestFile })

          pluginResources.from(
              plugin.sourceSetName.flatMap { project.sourceSets().named(it) }.map { it.resources }
          )
        }

    plugin.runtimeElements.configure {
      outgoing.variants.register("assets") {
        attributes.attribute(
            Category.CATEGORY_ATTRIBUTE,
            project.objects.named(dev.hygradle.internal.attributes.Category.PLUGIN_ASSETS),
        )
        artifact(plugin.assembleAssets.flatMap { it.assetDirectory }) {
          builtBy(plugin.assembleAssets)
        }
      }
    }

    // We don't know the sourceSet names until they're finalized. A necessary evil :(
    // TODO: Fix this shit if source sets ever get a proper lazy API
    project.afterEvaluate {
      project.sourceSets().named(plugin.sourceSetName.get()).configure {
        resources.srcDir(plugin.generateManifest)
      }

      project.tasks
          .named<ProcessResources>(
              project.sourceSets().getByName(plugin.sourceSetName.get()).processResourcesTaskName
          )
          .configure { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    }
  }
}
