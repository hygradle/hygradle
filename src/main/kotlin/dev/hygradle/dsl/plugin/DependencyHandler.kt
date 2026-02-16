package dev.hygradle.dsl.plugin

import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.tasks.Internal

/** A custom dependency handler implementation for plugins. */
interface DependencyHandler : Dependencies {
  @get:Internal val runtimeOnly: DependencyCollector
  @get:Internal val compileOnly: DependencyCollector
}
