package dev.hygradle.plugin.manifest

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.dsl.plugin.manifest.Author
import dev.hygradle.dsl.plugin.manifest.Dependency
import dev.hygradle.dsl.plugin.manifest.Manifest
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

abstract class ManifestImpl
@Inject
constructor(plugin: LatePlugin, private val objects: ObjectFactory, project: Project) : Manifest {
  init {
    name.convention(plugin.name)
    group.convention(project.group.toString())
    version.convention(project.version.toString())
  }

  override fun author(configure: Action<in Author>) {
    val author = objects.newInstance(Author::class.java)
    configure.execute(author)
    authors.add(author)
  }

  override fun dependency(configure: Action<in Dependency>) {
    val dependency = objects.newInstance(DependencyImpl::class.java)
    configure.execute(dependency)
    dependencies.add(dependency)
  }
}
