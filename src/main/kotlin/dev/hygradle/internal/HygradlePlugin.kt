package dev.hygradle.internal

import dev.hygradle.internal.extension.HygradleExtension
import dev.hygradle.internal.extension.PluginTaskRegistry
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.subsystem.ConventionPlugin
import dev.hygradle.internal.subsystem.DependencyPlugin
import dev.hygradle.internal.subsystem.PluginTaskPlugin
import dev.hygradle.internal.subsystem.RunTaskPlugin
import dev.hygradle.internal.subsystem.SettingsConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.create

@Suppress("Unused")
class HygradlePlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        extensions.create<HygradleExtension>("hygradle")

        (project.hygradle() as ExtensionAware)
            .extensions
            .create<PluginTaskRegistry>("_hygradle_taskRegistry")

        with(plugins) {
          apply(SettingsConventionPlugin::class.java)
          apply(ConventionPlugin::class.java)
          apply(DependencyPlugin::class.java)
          apply(PluginTaskPlugin::class.java)
          apply(RunTaskPlugin::class.java)
        }
      }
}
