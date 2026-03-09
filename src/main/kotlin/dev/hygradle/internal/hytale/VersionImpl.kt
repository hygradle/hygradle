@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.hytale

import dev.hygradle.dsl.hytale.Patchline
import dev.hygradle.dsl.hytale.Version
import javax.inject.Inject
import org.gradle.api.Project

abstract class VersionImpl @Inject constructor(project: Project) : Version {
  override val hytaleOnly = project.configurations.dependencyScope("hytaleOnly")

  override val hytaleClasspath =
      project.configurations.resolvable("hytaleClasspath") { extendsFrom(hytaleOnly) }

  init {
    patchline.convention(Patchline.RELEASE)
    decompile.convention(false)

    project.dependencies.addProvider(
        hytaleOnly.name,
        version.map { project.dependencies.create("com.hypixel.hytale:Server:${it}") },
    )
  }
}
