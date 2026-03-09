@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.extension

import dev.hygradle.dsl.extension.HotswapAgent
import javax.inject.Inject
import org.gradle.api.Project

abstract class HotswapAgentExtension @Inject constructor(project: Project) : HotswapAgent {
  override val hotswapAgentOnly = project.configurations.dependencyScope("hotswapAgentOnly")

  override val hotswapAgentClasspath =
      project.configurations.resolvable("hotswapAgentClasspath") { extendsFrom(hotswapAgentOnly) }

  init {
    version.convention("2.0.3")

    project.dependencies.addProvider(
        hotswapAgentOnly.name,
        version.map { project.dependencies.create("org.hotswapagent:hotswap-agent-core:$it") },
    )
  }
}
