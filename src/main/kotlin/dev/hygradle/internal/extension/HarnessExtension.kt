@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.extension

import dev.hygradle.dsl.extension.Harness
import javax.inject.Inject
import org.gradle.api.Project

abstract class HarnessExtension @Inject constructor(project: Project) : Harness {
  override val harnessOnly = project.configurations.dependencyScope("harnessOnly")

  override val harnessClasspath =
      project.configurations.resolvable("harnessClasspath") { extendsFrom(harnessOnly) }

  init {
    version.convention("0.0.1")

    project.dependencies.addProvider(
        harnessOnly.name,
        version.map { project.dependencies.create("dev.hygradle:harness:$it") },
    )
  }
}
