@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.extension

import dev.hygradle.dsl.extension.HotswapAgent
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

abstract class HotswapAgentExtension @Inject constructor(project: Project) : HotswapAgent {
  override val hotswapAgentOnly: NamedDomainObjectProvider<out Configuration> =
      project.configurations.dependencyScope("hotswapAgentOnly")

  override val hotswapAgentClasspath: NamedDomainObjectProvider<out Configuration> =
      project.configurations.resolvable("hotswapAgentClasspath")

  init {
    version.convention("2.0.3")

    hotswapAgentClasspath.configure { extendsFrom(hotswapAgentOnly) }

    project.dependencies.addProvider(
        hotswapAgentOnly.name,
        version.map { project.dependencies.create("org.hotswapagent:hotswap-agent-core:$it") },
    )
  }
}
