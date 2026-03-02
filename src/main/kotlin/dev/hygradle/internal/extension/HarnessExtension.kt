@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.extension

import dev.hygradle.dsl.extension.Harness
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

abstract class HarnessExtension @Inject constructor(project: Project) : Harness {
  override val harnessOnly: NamedDomainObjectProvider<out Configuration> =
      project.configurations.dependencyScope("harnessOnly")

  override val harnessClasspath: NamedDomainObjectProvider<out Configuration> =
      project.configurations.resolvable("harnessClasspath")

  init {
    version.convention("0.0.1")

    harnessClasspath.configure { extendsFrom(harnessOnly) }

    project.dependencies.addProvider(
        harnessOnly.name,
        version.map { project.dependencies.create("dev.hygradle:harness:$it") },
    )
  }
}
