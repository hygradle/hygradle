package dev.hygradle.dsl.plugin.manifest

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface Dependency {
  @get:Input val name: Property<String>
  @get:Input val group: Property<String>
  @get:Input val version: Property<String>
  @get:Input val optional: Property<Boolean>
}
