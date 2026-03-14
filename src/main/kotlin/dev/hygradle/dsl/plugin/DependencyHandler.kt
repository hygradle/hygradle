package dev.hygradle.dsl.plugin

import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector

/** A custom dependency handler implementation for plugins. */
interface DependencyHandler : Dependencies {
  val runtimeOnly: DependencyCollector
  val compileOnly: DependencyCollector
  val plugin: DependencyCollector
}
