package dev.hygradle.internal.plugin

import dev.hygradle.dsl.plugin.PluginDependencies
import javax.inject.Inject
import org.gradle.api.artifacts.ProjectDependency

@Suppress("UnstableApiUsage")
abstract class PluginDependenciesImpl @Inject constructor(private val plugin: PluginImpl) :
    PluginDependencies {
  init {
    plugin.compileOnly.configure { fromDependencyCollector(compileOnly) }
    plugin.runtimeOnly.configure { fromDependencyCollector(runtimeOnly) }
  }

  override fun compilePlugin(dep: ProjectDependency, pluginName: String) =
      plugin.compileOnly.configure {
        dependencies.add(dep.capabilities { requireCapability("dev.hygradle.plugin:$pluginName") })
      }

  override fun runtimePlugin(dep: ProjectDependency, pluginName: String) =
      plugin.runtimeOnly.configure {
        dependencies.add(dep.capabilities { requireCapability("dev.hygradle.plugin:$pluginName") })
      }
}
