package dev.hygradle.internal.plugin.manifest

import dev.hygradle.dsl.plugin.manifest.Dependency

abstract class DependencyImpl : Dependency {
  init {
    optional.convention(false)
  }
}
