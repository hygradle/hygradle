@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.subsystem

import dev.hygradle.internal.HygradleAttributes
import dev.hygradle.internal.HygradleVariant
import dev.hygradle.internal.extension.globalTaskRegistry
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.extension.hygradleConfigurations
import dev.hygradle.internal.extension.pluginTaskRegistry
import dev.hygradle.internal.plugin.sourceSets
import dev.hygradle.internal.task.run.PrepareRunDirectory
import dev.hygradle.internal.task.run.RunHytaleServer
import dev.hygradle.internal.util.capitalize
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class RunTaskPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val hygradle = project.hygradle()
    val configurations = project.hygradleConfigurations()
    val globalRegistry = project.gradle.globalTaskRegistry()
    val registry = project.pluginTaskRegistry()
    val sourceSets = project.sourceSets()
    val objects = project.objects
    val providers = project.providers
    val pluginContainer = hygradle.plugins
    val runs = hygradle.runs

    runs.all {
      val run = this

      run.plugins.convention(providers.provider { pluginContainer.names })

      val externalPluginsScope =
          project.configurations.dependencyScope("_${name}ExternalPlugins") {
            fromDependencyCollector(run.externalPlugins)
          }

      val externalPluginClasspath =
          project.configurations.resolvable("_${name}ExternalPluginClasspath") {
            extendsFrom(externalPluginsScope)
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

      val prepareRunDirectory =
          project.tasks.register<PrepareRunDirectory>("prepare${name.capitalize()}RunDirectory") {
            group = "hygradle/runs/${run.name}"
            runName.set(run.name)
          }

      project.tasks.register<RunHytaleServer>("start${name.capitalize()}Server") {
        group = "hygradle/runs/${run.name}"

        if (globalRegistry.generateSources != null) {
          dependsOn(globalRegistry.generateSources!!)
        }

        runDirectory.set(prepareRunDirectory.flatMap { it.runDirectory })
        classpathProvider.from(configurations.hytaleClasspath)

        assets.from(globalRegistry.extractAssets.map { it.assetCacheDirectory.asFileTree })
        hotswapAgent.from(configurations.hotswapAgentClasspath)
        harness.from(configurations.harnessClasspath)

        classpathProvider.from(externalPluginClasspath)

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

                  val assetTask = registry.assetTasks[name]
                  if (assetTask != null) from(assetTask.flatMap { it.assetDirectory })
                }
              }
            }
        )
      }
    }
  }
}
