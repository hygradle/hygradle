package dev.hygradle.dsl.plugin

import dev.hygradle.dsl.plugin.manifest.Manifest
import org.gradle.api.Action

interface LatePlugin : Plugin {
  val manifest: Manifest

  fun manifest(manifest: Action<in Manifest>)
}
