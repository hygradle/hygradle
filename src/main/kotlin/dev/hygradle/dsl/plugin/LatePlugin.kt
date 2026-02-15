package dev.hygradle.dsl.plugin

import dev.hygradle.dsl.plugin.manifest.Manifest
import org.gradle.api.Action
import org.gradle.api.tasks.Nested

interface LatePlugin : Plugin {
  @get:Nested val manifest: Manifest

  fun manifest(manifest: Action<in Manifest>)
}
