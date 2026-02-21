package dev.hygradle.dsl.hytale

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property

interface Version {
  /** The [Patchline] for the target Hytale version */
  val patchline: Property<Patchline>

  /** The Hytale version to use for game runs. */
  val version: Property<String>

  val decompile: Property<Boolean>

  val hytaleOnly: NamedDomainObjectProvider<out Configuration>

  val hytaleClasspath: NamedDomainObjectProvider<out Configuration>
}
