package dev.hygradle.internal.plugin

import dev.hygradle.dsl.plugin.DependencyHandler
import dev.hygradle.dsl.plugin.Plugin
import javax.inject.Inject

@Suppress("UnstableApiUsage")
abstract class DependencyHandlerImpl @Inject internal constructor(plugin: Plugin) :
    DependencyHandler {
  init {
    plugin.runtimeOnlyConfiguration.configure { fromDependencyCollector(runtimeOnly) }
    plugin.compileOnlyConfiguration.configure { fromDependencyCollector(compileOnly) }
  }
}
