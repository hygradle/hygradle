package dev.hygradle.internal.plugin.manifest

import dev.hygradle.dsl.plugin.manifest.Author
import dev.hygradle.dsl.plugin.manifest.Dependency
import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.internal.extension.hygradle
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class ManifestImpl
@Inject
internal constructor(pluginName: String, private val objects: ObjectFactory, project: Project) :
    Manifest {
  init {
    name.convention(pluginName)
    group.convention(project.provider { project.group.toString() })
    version.convention(project.provider { project.version.toString() })
    serverVersion.convention(project.hygradle().hytale.version)
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
