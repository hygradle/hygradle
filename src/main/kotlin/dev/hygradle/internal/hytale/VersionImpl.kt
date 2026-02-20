@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.hytale

import dev.hygradle.dsl.hytale.Patchline
import dev.hygradle.dsl.hytale.Version
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

abstract class VersionImpl @Inject constructor(project: Project) : Version {
  override val hytaleOnly: NamedDomainObjectProvider<out Configuration> =
      project.configurations.dependencyScope("hytaleOnly")

  override val hytaleClasspath: NamedDomainObjectProvider<out Configuration> =
      project.configurations.resolvable("hytaleClasspath")

  init {
    patchline.convention(Patchline.RELEASE)
    decompile.convention(false)

    hytaleClasspath.configure { extendsFrom(hytaleOnly) }

    project.dependencies.addProvider(
        hytaleOnly.name,
        version.map { project.dependencies.create("com.hypixel.hytale:Server:${it}") },
    )
  }
}
