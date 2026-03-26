package dev.hygradle.internal

import dev.hygradle.dsl.extension.Hygradle
import dev.hygradle.internal.extension.HygradleExtension
import dev.hygradle.internal.system.AttributeSystem
import dev.hygradle.internal.system.ConventionSystem
import dev.hygradle.internal.system.DependencySystem
import dev.hygradle.internal.system.PluginSystem
import dev.hygradle.internal.system.RunSystem
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.newInstance

@Suppress("Unused")
class HygradlePlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        extensions.add(Hygradle::class.java, "hygradle", objects.newInstance<HygradleExtension>())

        with(plugins) {
          apply(ConventionSystem::class.java)
          apply(DependencySystem::class.java)
          apply(PluginSystem::class.java)
          apply(RunSystem::class.java)
          apply(AttributeSystem::class.java)
        }
      }
}
