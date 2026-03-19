package dev.hygradle.dsl.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet

/** The base for all plugin types. */
interface Plugin : Named {
  val sourceSetName: Property<String>

  /** Assign the primary source set to use for the plugin - defaults to the main source set. */
  fun sourceSet(sourceSet: SourceSet)

  fun sourceSet(sourceSet: Provider<SourceSet>)

  val dependencies: PluginDependencies

  /** Configure dependencies for this plugin. */
  fun dependencies(configure: Action<in PluginDependencies>)
}
