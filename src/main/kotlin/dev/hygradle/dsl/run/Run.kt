package dev.hygradle.dsl.run

import org.gradle.api.Named
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

/** A configured server run. */
interface Run : Named {
  /** Plugin names to include in this run. Defaults to all registered plugins. */
  @get:Input val plugins: SetProperty<String>

  fun includePlugins(vararg plugins: String)
}
