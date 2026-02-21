package dev.hygradle.dsl.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet

/** The base for all plugin types. */
interface Plugin : Named {
  val sourceSetName: Property<String>

  /** Assign the primary source set to use for the plugin - defaults to the main source set. */
  fun sourceSet(sourceSet: SourceSet)

  fun sourceSet(sourceSet: Provider<SourceSet>)

  /** The compile-only configuration for the plugin. */
  val compileOnlyConfiguration: NamedDomainObjectProvider<out Configuration>

  /** The compile-time classpath for the plugin. */
  val compileClasspathConfiguration: NamedDomainObjectProvider<out Configuration>

  /** The runtime-only configuration for the plugin. */
  val runtimeOnlyConfiguration: NamedDomainObjectProvider<out Configuration>

  /** The runtime-only classpath for the plugin. */
  val runtimeClasspathConfiguration: NamedDomainObjectProvider<out Configuration>

  /** The custom [DependencyHandler] for this plugin. */
  val dependencies: DependencyHandler

  /** Configure the [DependencyHandler] for this plugin. */
  fun dependencies(configure: Action<in DependencyHandler>)
}
