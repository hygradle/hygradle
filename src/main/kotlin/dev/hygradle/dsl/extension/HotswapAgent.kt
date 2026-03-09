package dev.hygradle.dsl.extension

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.DependencyScopeConfiguration
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.provider.Property

interface HotswapAgent {
  val version: Property<String>

  val hotswapAgentOnly: NamedDomainObjectProvider<DependencyScopeConfiguration>

  val hotswapAgentClasspath: NamedDomainObjectProvider<ResolvableConfiguration>
}
