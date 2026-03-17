package dev.hygradle.internal.plugin

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.internal.plugin.manifest.ManifestImpl
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class LatePluginImpl(name: String) : PluginImpl(name), LatePlugin {

  @get:Inject abstract val objects: ObjectFactory

  override val manifest: Manifest =
      objects.newInstance<ManifestImpl>().also { it.name.convention(name) }

  override fun manifest(manifest: Action<in Manifest>) = manifest.execute(this.manifest)
}
