package dev.hygradle.internal.plugin

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.internal.plugin.delegate.Plugin as PluginDelegate
import dev.hygradle.plugin.manifest.ManifestImpl
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class LatePluginImpl
@Inject
constructor(private val name: String, objects: ObjectFactory, project: Project) :
    Plugin by PluginDelegate(name, project), LatePlugin {
  override val manifest: Manifest = objects.newInstance<ManifestImpl>(this)

  override fun manifest(manifest: Action<in Manifest>) = manifest.execute(this.manifest)
}
