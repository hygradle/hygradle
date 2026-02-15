package dev.hygradle.dsl.plugin.manifest

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface Author {
  @get:Input val name: Property<String>
  @get:Input val email: Property<String>
  @get:Input val url: Property<String>
}
