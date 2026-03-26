package dev.hygradle.internal.plugin

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.internal.plugin.manifest.ManifestImpl
import dev.hygradle.internal.task.plugin.AssembleAssets
import dev.hygradle.internal.task.plugin.GenerateManifest
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.newInstance

abstract class LatePluginImpl(name: String) : PluginImpl(name), LatePlugin {

  @get:Inject abstract val objects: ObjectFactory

  lateinit var generateManifest: TaskProvider<GenerateManifest>

  lateinit var assembleAssets: TaskProvider<AssembleAssets>

  override val manifest: Manifest =
      objects.newInstance<ManifestImpl>().also { it.name.convention(name) }

  override fun manifest(manifest: Action<in Manifest>) = manifest.execute(this.manifest)

  context(project: Project)
  override fun configureConventions() {
    manifest.includesAssetPack.convention(
        sourceSetName
            .flatMap { project.sourceSets().named(it) }

            // TODO: Surely this can be simplified
            .map { sourceSet ->
              var found = false

              sourceSet.resources.asFileTree.visit {
                if (
                    relativePath.segments.size == 1 && isDirectory && name in ASSET_DIRECTORY_NAMES
                ) {
                  found = true
                  stopVisiting()
                }
              }

              found
            }
    )
  }

  companion object {
    private val ASSET_DIRECTORY_NAMES = listOf("Common", "Server")
  }
}
