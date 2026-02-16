package dev.hygradle.dsl.plugin.manifest

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/** Information for a credited plugin author. */
interface Author {
  /** The name of the author. */
  @get:Input val name: Property<String>

  /** The email of the author. */
  @get:Input val email: Property<String>

  /** The preferred URL to represent the author. */
  @get:Input val url: Property<String>
}
