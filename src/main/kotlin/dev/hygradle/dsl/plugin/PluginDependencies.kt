package dev.hygradle.dsl.plugin

import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector

interface PluginDependencies : Dependencies {
  val runtimeOnly: DependencyCollector
  val compileOnly: DependencyCollector
  val plugin: DependencyCollector
  val optionalPlugin: DependencyCollector
}
