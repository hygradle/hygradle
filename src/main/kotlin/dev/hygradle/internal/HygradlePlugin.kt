@file:Suppress("Unused", "UnstableApiUsage")

package dev.hygradle.internal

import dev.hygradle.internal.extension.HygradleExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginAware
import org.gradle.kotlin.dsl.create

class HygradlePlugin : Plugin<PluginAware> {
  override fun apply(target: PluginAware): Unit =
      when (target) {
        is Project -> apply(target)
        else -> throw GradleException("Hygradle must be applied at the project level.")
      }

  private fun apply(project: Project): Unit =
      with(project) {
        extensions.create<HygradleExtension>("hygradle")

        with(plugins) {
          apply(ConventionPlugin::class.java)
          apply(RepositoryPlugin::class.java)
          apply(DependencyPlugin::class.java)
          apply(TaskPlugin::class.java)
        }
      }
}
