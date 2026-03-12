package dev.hygradle.dsl.settings

import org.gradle.api.provider.Property

interface VersionedDependency {
  val version: Property<String>
}
