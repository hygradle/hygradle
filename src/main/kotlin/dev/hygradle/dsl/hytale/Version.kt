package dev.hygradle.dsl.hytale

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface Version {
  /** The [Patchline] for the target Hytale version */
  @get:Input val patchline: Property<Patchline>

  /** The Hytale version to use for game runs. */
  @get:Input val version: Property<String>
}
