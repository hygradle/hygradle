package dev.hygradle.dsl.plugin

import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.tasks.GeneratePluginManifest
import dev.hygradle.tasks.PreparePluginAssets
import org.gradle.api.Action
import org.gradle.api.provider.Property

interface LatePlugin : Plugin {
  val manifest: Manifest

  fun manifest(manifest: Action<in Manifest>)

  val generateManifest: Property<GeneratePluginManifest>

  val prepareAssets: Property<PreparePluginAssets>
}
