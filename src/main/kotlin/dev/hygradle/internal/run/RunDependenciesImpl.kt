@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.run

import dev.hygradle.dsl.run.RunDependencies
import javax.inject.Inject
import org.gradle.api.artifacts.ProjectDependency

abstract class RunDependenciesImpl @Inject constructor(private val run: RunImpl) : RunDependencies {
  init {
    run.runtimeOnly.configure { fromDependencyCollector(runtimeOnly) }
  }

  override fun runtimePlugin(dep: ProjectDependency, pluginName: String) =
      run.runtimeOnly.configure {
        dependencies.add(dep.capabilities { requireCapability("dev.hygradle.plugin:$pluginName") })
      }
}
