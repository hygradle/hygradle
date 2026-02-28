package dev.hygradle.dsl.plugin

import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.internal.task.plugin.AssembleAssets
import dev.hygradle.internal.task.plugin.GenerateManifest
import org.gradle.api.Action
import org.gradle.api.provider.Property

interface LatePlugin : Plugin {
  val manifest: Manifest

  fun manifest(manifest: Action<in Manifest>)

  val generateManifest: Property<GenerateManifest>

  val assembleAssets: Property<AssembleAssets>
}
