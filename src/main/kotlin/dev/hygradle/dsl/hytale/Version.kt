package dev.hygradle.dsl.hytale

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.DependencyScopeConfiguration
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.provider.Property

interface Version {
  /** The [Patchline] for the target Hytale version */
  val patchline: Property<Patchline>

  /** The Hytale version to use for game runs. */
  val version: Property<String>

  val decompile: Property<Boolean>

  val hytaleOnly: NamedDomainObjectProvider<DependencyScopeConfiguration>

  val hytaleClasspath: NamedDomainObjectProvider<ResolvableConfiguration>
}
