package dev.hygradle.plugin

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.plugin.manifest.ManifestImpl
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class LatePluginImpl
@Inject
constructor(private val name: String, objects: ObjectFactory) : LatePlugin {
  override fun getName(): String = name

  override val manifest = objects.newInstance<ManifestImpl>(this)

  override fun manifest(manifest: Action<in Manifest>): Unit = manifest.execute(this.manifest)
}
