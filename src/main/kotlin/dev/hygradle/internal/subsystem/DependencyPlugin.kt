@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.subsystem

import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.extension.hygradleConfigurations
import dev.hygradle.internal.plugin.sourceSets
import org.gradle.api.Plugin as GradlePlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType

class DependencyPlugin : GradlePlugin<Project> {
  override fun apply(project: Project) {
    project.hygradle().plugins.withType<Plugin>().all {
      val pluginConfig = project.configurations.named("${name}Plugin")
      project.configurations
          .named(project.sourceSets().getByName(sourceSetName.get()).compileOnlyConfigurationName)
          .configure {
            extendsFrom(project.hygradleConfigurations().hytaleOnly)
            extendsFrom(pluginConfig)
          }
    }
  }
}
