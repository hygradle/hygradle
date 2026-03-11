package dev.hygradle.internal.run

import dev.hygradle.dsl.run.Run

abstract class RunImpl : Run {
  override fun includePlugins(vararg plugins: String): Unit = this.plugins.addAll(*plugins)
}
