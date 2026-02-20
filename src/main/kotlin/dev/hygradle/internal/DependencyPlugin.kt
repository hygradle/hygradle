@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal

import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.internal.extension.hygradle
import org.gradle.api.Plugin as GradlePlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType

class DependencyPlugin : GradlePlugin<Project> {
  override fun apply(project: Project) {
    project.hygradle().plugins.withType<Plugin>().all {
      project.configurations.named(sourceSet.get().compileOnlyConfigurationName).configure {
        extendsFrom(project.hygradle().hytale.hytaleOnly)
      }
    }
  }
}
