package dev.hygradle.dsl.hytale

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

interface Version {
  @get:Input val patchline: Property<Patchline>

  @get:Input val version: Property<String>
}
