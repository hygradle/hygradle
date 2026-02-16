package dev.hygradle

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.internal.extension.HygradleExtension
import dev.hygradle.tasks.GeneratePluginManifest
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginAware
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
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

    ext.plugins.withType<LatePlugin>().all {
      val generateManifest =
          project.tasks.register<GeneratePluginManifest>("${name}GenerateManifest") {
            group = "hygradle/plugins/${name}"
          }
    }
  }
}
