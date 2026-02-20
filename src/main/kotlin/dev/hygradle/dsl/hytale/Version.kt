package dev.hygradle.dsl.hytale

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

interface Version {
  /** The [Patchline] for the target Hytale version */
  @get:Input val patchline: Property<Patchline>

  /** The Hytale version to use for game runs. */
  @get:Input val version: Property<String>

  @get:Input val decompile: Property<Boolean>

  @get:Internal val hytaleOnly: NamedDomainObjectProvider<out Configuration>

  @get:Internal val hytaleClasspath: NamedDomainObjectProvider<out Configuration>
}
