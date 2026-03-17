package dev.hygradle.internal

import dev.hygradle.internal.extension.HygradleExtension
import dev.hygradle.internal.subsystem.ConventionPlugin
import dev.hygradle.internal.subsystem.DependencyPlugin
import dev.hygradle.internal.subsystem.ServicePlugin
import dev.hygradle.internal.subsystem.SettingsConventionPlugin
import dev.hygradle.internal.subsystem.TaskPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginAware
import org.gradle.kotlin.dsl.create

@Suppress("Unused")
class HygradlePlugin : Plugin<PluginAware> {
  override fun apply(target: PluginAware): Unit =
      when (target) {
        is Project -> apply(target)
        else -> throw GradleException("Hygradle must be applied at the project level.")
      }

  private fun apply(project: Project): Unit =
      with(project) {
        extensions.create<HygradleExtension>("hygradle")

        // TODO: Split this out into a separate subsystem?
        dependencies.attributesSchema {
          attribute(HygradleAttributes.VARIANT_ATTRIBUTE)
          attribute(HygradleAttributes.PLUGIN_NAME_ATTRIBUTE)
        }

        with(plugins) {
          apply(SettingsConventionPlugin::class.java)
          apply(ConventionPlugin::class.java)
          apply(DependencyPlugin::class.java)
          apply(TaskPlugin::class.java)
          apply(ServicePlugin::class.java)
        }
      }
}
