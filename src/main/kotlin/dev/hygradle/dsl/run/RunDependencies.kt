package dev.hygradle.dsl.run

import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector

interface RunDependencies : Dependencies {
  val runtimeOnly: DependencyCollector

  fun runtimePlugin(dep: ProjectDependency, pluginName: String)
}
