package dev.hygradle

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.internal.extension.HygradleExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginAware

abstract class HygradlePlugin : Plugin<PluginAware> {
  override fun apply(target: PluginAware): Unit =
      when (target) {
        is Project -> apply(target)
        else -> throw GradleException("Hygradle must be applied at the project level.")
      }

  private fun apply(project: Project) {
    val ext = project.extensions.create("hygradle", HygradleExtension::class.java)

    ext.plugins.withType(LatePlugin::class.java).all {
      val generateManifest =
          project.tasks.register("${name}GenerateManifest") { group = "hygradle/plugins/${name}" }

      val runtimeOnly = project.configurations.dependencyScope("${name}RuntimeOnly")
      val compileOnly = project.configurations.dependencyScope("${name}CompileOnly")
      val implementation = project.configurations.dependencyScope("${name}Implementation")

      val runtimeClasspath =
          project.configurations.resolvable("${name}RuntimeClasspath") {
            extendsFrom(implementation.get(), runtimeOnly.get())
          }

      val compileClasspath =
          project.configurations.resolvable("${name}CompileClasspath") {
            extendsFrom(implementation.get(), compileOnly.get())
          }

      //      runtimeOnlyConfiguration.set(runtimeOnly)
      //      compileOnlyConfiguration.set(compileOnly)
      //      runtimeClasspathConfiguration.set(runtimeClasspath)
      //      compileClasspathConfiguration.set(compileClasspath)
    }
  }
}
