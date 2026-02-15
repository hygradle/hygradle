package dev.hygradle.plugin

import dev.hygradle.dsl.plugin.Plugin
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet

@Suppress("UnstableApiUsage")
class PluginDelegate(private val name: String, private val project: Project) : Plugin {
  override fun getName(): String = name

  override val runtimeOnlyConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.dependencyScope("${name}PluginRuntimeOnly") {
        description = "Runtime-only dependencies for the Hytale plugin '$name'."
      }

  override val runtimeClasspathConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.resolvable("${name}PluginRuntimeClasspath")

  override val compileOnlyConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.dependencyScope("${name}PluginCompileOnly") {
        description = "Compile-only dependencies for the Hytale plugin '$name'."
      }

  override val compileClasspathConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.resolvable("${name}PluginCompileClasspath")

  override fun sourceSet(sourceSet: SourceSet) {
    runtimeOnlyConfiguration.configure {
      extendsFrom(project.configurations.getByName(sourceSet.runtimeOnlyConfigurationName))
    }

    compileClasspathConfiguration.configure {
      extendsFrom(project.configurations.getByName(sourceSet.compileOnlyConfigurationName))
    }
  }

  override fun sourceSet(sourceSet: Provider<SourceSet>) = sourceSet(sourceSet.get())
}
