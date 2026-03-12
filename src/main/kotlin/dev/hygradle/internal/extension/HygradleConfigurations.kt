@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.extension

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

abstract class HygradleConfigurations @Inject constructor(project: Project) {
  val hytaleOnly = project.configurations.dependencyScope("hytaleOnly")

  val hytaleClasspath =
      project.configurations.resolvable("hytaleClasspath") { extendsFrom(hytaleOnly) }

  val harnessOnly = project.configurations.dependencyScope("harnessOnly")

  val harnessClasspath =
      project.configurations.resolvable("harnessClasspath") { extendsFrom(harnessOnly) }

  val hotswapAgentOnly = project.configurations.dependencyScope("hotswapAgentOnly")

  val hotswapAgentClasspath =
      project.configurations.resolvable("hotswapAgentClasspath") { extendsFrom(hotswapAgentOnly) }
}

internal fun Project.hygradleConfigurations(): HygradleConfigurations =
    (hygradle() as ExtensionAware).extensions.getByType(HygradleConfigurations::class.java)
