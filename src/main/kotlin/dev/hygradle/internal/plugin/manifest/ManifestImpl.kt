package dev.hygradle.internal.plugin.manifest

import dev.hygradle.dsl.plugin.manifest.Author
import dev.hygradle.dsl.plugin.manifest.Dependency
import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.internal.service.settings.settingsService
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class ManifestImpl : Manifest {

  @get:Inject abstract val objects: ObjectFactory

  @get:Inject abstract val project: Project

  init {
    group.convention(project.group.toString().ifEmpty { null })
    version.convention(project.version.toString().takeIf { it != Project.DEFAULT_VERSION })
    serverVersion.convention(project.settingsService().hytaleVersion)
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
