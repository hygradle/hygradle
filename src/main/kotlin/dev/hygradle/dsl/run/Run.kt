package dev.hygradle.dsl.run

import org.gradle.api.Named
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

/** A configured server run. */
interface Run : Named, Dependencies {
  /** Plugin names to include in this run. Defaults to all registered plugins. */
  @get:Input val plugins: SetProperty<String>

  fun includePlugins(vararg plugins: String)

  /** External plugin projects to include on the run classpath. */
  val externalPlugins: DependencyCollector
}
