package dev.hygradle.internal.run

import dev.hygradle.dsl.run.Run
import java.util.Locale.getDefault

abstract class RunImpl(private val name: String) : Run {
  val taskGroup = "hygradle/runs/$name"

  val taskSlug =
      name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }

  override fun getName(): String = name

  override fun includePlugins(vararg plugins: String): Unit = this.plugins.addAll(*plugins)
}
