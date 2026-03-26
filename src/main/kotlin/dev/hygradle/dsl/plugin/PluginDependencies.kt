package dev.hygradle.dsl.plugin

import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector

interface PluginDependencies : Dependencies {
  val runtimeOnly: DependencyCollector

  val compileOnly: DependencyCollector

  fun runtimePlugin(dep: ProjectDependency, pluginName: String)

  fun compilePlugin(dep: ProjectDependency, pluginName: String)
}
