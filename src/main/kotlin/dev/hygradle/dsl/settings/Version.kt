package dev.hygradle.dsl.settings

import dev.hygradle.dsl.hytale.Patchline
import org.gradle.api.provider.Property

interface Version {
  val patchline: Property<Patchline>

  val version: Property<String>

  val decompile: Property<Boolean>
}
