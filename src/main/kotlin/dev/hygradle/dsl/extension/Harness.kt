package dev.hygradle.dsl.extension

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property

interface Harness {
  val version: Property<String>

  val harnessOnly: NamedDomainObjectProvider<out Configuration>

  val harnessClasspath: NamedDomainObjectProvider<out Configuration>
}
