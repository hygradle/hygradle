package dev.hygradle.internal.plugin

import dev.hygradle.dsl.plugin.PluginDependencies
import javax.inject.Inject

@Suppress("UnstableApiUsage")
abstract class PluginDependenciesImpl @Inject constructor(plugin: PluginImpl) : PluginDependencies {
  init {
    plugin.compileOnlyConfiguration.configure { fromDependencyCollector(compileOnly) }
    plugin.runtimeOnlyConfiguration.configure { fromDependencyCollector(runtimeOnly) }
    plugin.pluginConfiguration.configure {
      fromDependencyCollector(this@PluginDependenciesImpl.plugin)
    }
    plugin.optionalPluginConfiguration.configure { fromDependencyCollector(optionalPlugin) }
  }
}
