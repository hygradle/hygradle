package dev.hygradle.dsl.extension

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.DependencyScopeConfiguration
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.provider.Property

interface Harness {
  val version: Property<String>

  val harnessOnly: NamedDomainObjectProvider<DependencyScopeConfiguration>

  val harnessClasspath: NamedDomainObjectProvider<ResolvableConfiguration>
}
