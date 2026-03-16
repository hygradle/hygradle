package dev.hygradle.internal.plugin.manifest

import dev.hygradle.dsl.plugin.manifest.Author
import dev.hygradle.dsl.plugin.manifest.Dependency
import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.internal.subsystem.hygradleSettings
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class ManifestImpl @Inject internal constructor(pluginName: String) : Manifest {

  @get:Inject internal abstract val objects: ObjectFactory

  @get:Inject internal abstract val project: Project

  init {
    name.convention(pluginName)
    group.convention(project.providers.gradleProperty("group").orNull)
    version.convention(project.providers.gradleProperty("version").orNull)
    serverVersion.convention(project.hygradleSettings().hytale.version)
    includesAssetPack.convention(false)
  }

  override fun author(configure: Action<in Author>) {
    objects.newInstance<Author>().also {
      configure.execute(it)
      authors.add(it)
    }
  }

  override fun dependency(configure: Action<in Dependency>) {
    objects.newInstance<DependencyImpl>().also {
      configure.execute(it)
      dependencies.add(it)
    }
  }
}
