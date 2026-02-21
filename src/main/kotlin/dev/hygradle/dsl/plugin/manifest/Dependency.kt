package dev.hygradle.dsl.plugin.manifest

import org.gradle.api.provider.Property

interface Dependency {
  val name: Property<String>
  val group: Property<String>
  val version: Property<String>
  val optional: Property<Boolean>
}
