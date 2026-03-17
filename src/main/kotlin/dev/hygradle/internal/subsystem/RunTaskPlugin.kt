@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.subsystem

import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.extension.hygradleConfigurations
import dev.hygradle.internal.extension.pluginTaskRegistry
import dev.hygradle.internal.plugin.sourceSets
import dev.hygradle.internal.task.run.PrepareRunDirectory
import dev.hygradle.internal.task.run.RunHytaleServer
import dev.hygradle.internal.util.capitalize
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class RunTaskPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val hygradle = project.hygradle()
    val configurations = project.hygradleConfigurations()
    val registry = project.pluginTaskRegistry()
    val sourceSets = project.sourceSets()
    val objects = project.objects
    val providers = project.providers
    val pluginContainer = hygradle.plugins
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

        if (registry.generateSources != null) {
          dependsOn(registry.generateSources!!)
        }

        runDirectory.set(prepareRunDirectory.flatMap { it.runDirectory })
        classpathProvider.from(configurations.hytaleClasspath)

        assets.from(registry.extractAssets.map { it.assetCacheDirectory.asFileTree })
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
