package dev.hygradle.dsl.plugin

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet

interface Plugin : Named {
  @get:Internal val compileOnlyConfiguration: NamedDomainObjectProvider<out Configuration>
  @get:Internal val compileClasspathConfiguration: NamedDomainObjectProvider<out Configuration>
  @get:Internal val runtimeOnlyConfiguration: NamedDomainObjectProvider<out Configuration>
  @get:Internal val runtimeClasspathConfiguration: NamedDomainObjectProvider<out Configuration>

  fun sourceSet(sourceSet: SourceSet)

  fun sourceSet(sourceSet: Provider<SourceSet>)
}
