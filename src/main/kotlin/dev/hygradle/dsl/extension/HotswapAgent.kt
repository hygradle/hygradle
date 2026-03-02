package dev.hygradle.dsl.extension

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property

interface HotswapAgent {
  val version: Property<String>

  val hotswapAgentOnly: NamedDomainObjectProvider<out Configuration>

  val hotswapAgentClasspath: NamedDomainObjectProvider<out Configuration>
}
