package dev.hygradle.dsl.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.SourceSet

/** The base for all plugin types. */
interface Plugin : Named {
  /** The compile-only configuration for the plugin. */
  @get:Internal val compileOnlyConfiguration: NamedDomainObjectProvider<out Configuration>

  /** The compile-time classpath for the plugin. */
  @get:Internal val compileClasspathConfiguration: NamedDomainObjectProvider<out Configuration>

  /** The runtime-only configuration for the plugin. */
  @get:Internal val runtimeOnlyConfiguration: NamedDomainObjectProvider<out Configuration>

  /** The runtime-only classpath for the plugin. */
  @get:Internal val runtimeClasspathConfiguration: NamedDomainObjectProvider<out Configuration>

  /** The custom [DependencyHandler] for this plugin. */
  @get:Nested val dependencies: DependencyHandler

  /** Configure the [DependencyHandler] for this plugin. */
  fun dependencies(configure: Action<in DependencyHandler>)

  /** Assign the primary source set to use for the plugin. */
  fun sourceSet(sourceSet: SourceSet)

  fun sourceSet(sourceSet: Provider<SourceSet>)
}
