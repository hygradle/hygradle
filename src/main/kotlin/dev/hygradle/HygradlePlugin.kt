package dev.hygradle

import dev.hygradle.internal.extension.HygradleExtension
import java.net.URI
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginAware
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType

@Suppress("Unused")
abstract class HygradlePlugin : Plugin<PluginAware> {
  override fun apply(target: PluginAware): Unit =
      when (target) {
        is Project -> apply(target)
        else -> throw GradleException("Hygradle must be applied at the project level.")
      }

  private fun apply(project: Project) {
    val ext = project.extensions.create<HygradleExtension>("hygradle")

    ext.plugins.withType<dev.hygradle.dsl.plugin.Plugin>().all {
      project.dependencies.add(
          sourceSetCompileOnlyConfigurationName.get(),
          project.dependencies.create("com.hypixel.hytale:Server:${ext.hytale.version.get()}"),
      )
      //      val generateManifest =
      //          project.tasks.register<GeneratePluginManifest>("${name}GenerateManifest") {
      //            group = "hygradle/plugins/${name}"
      //          }
    }

    project.repositories.add(
        ext.hytale.patchline
            .map {
              project.repositories.maven {
                name = "hytale-${it.name.lowercase()}"
                url = URI.create(it.repository)
              }
            }
            .get()
    )
  }
}
