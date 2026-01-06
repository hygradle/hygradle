package dev.hygradle.dsl.ide

import org.gradle.api.Project

public sealed class IdeExtension {
  public companion object {
    private const val EXTENSION_NAME = "hygradleIdeExtension"

    public fun Project.ideExtension(): IdeExtension =
        extensions.findByType(IdeExtension::class.java)
            ?: when {
              isIntellij() -> IntellijExtension(this)
              isEclipse() -> EclipseExtension(this)
              else -> None()
            }.also { this.extensions.add(IdeExtension::class.java, EXTENSION_NAME, it) }

    private fun isIntellij() = System.getProperty("idea.active") != null

    private fun isEclipse() = System.getProperty("eclipse.application") != null
  }

  public class None : IdeExtension()
}
