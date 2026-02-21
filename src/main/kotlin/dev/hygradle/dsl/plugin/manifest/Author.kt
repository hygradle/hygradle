package dev.hygradle.dsl.plugin.manifest

import org.gradle.api.provider.Property

/** Information for a credited plugin author. */
interface Author {
  /** The name of the author. */
  val name: Property<String>

  /** The email of the author. */
  val email: Property<String>

  /** The preferred URL to represent the author. */
  val url: Property<String>
}
