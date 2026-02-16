package dev.hygradle.internal.plugin.delegate

import dev.hygradle.dsl.plugin.DependencyHandler
import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.internal.plugin.DependencyHandlerImpl
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.newInstance

@Suppress("UnstableApiUsage")
class Plugin(private val name: String, private val project: Project) : Plugin {
  override fun getName(): String = name

  override val runtimeOnlyConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.dependencyScope("${name}RuntimeOnly") {
        description = "Runtime-only dependencies for the Hytale plugin '${this@Plugin.name}'."
      }

  override val runtimeClasspathConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.resolvable("${name}RuntimeClasspath")

  override val compileOnlyConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.dependencyScope("${name}CompileOnly") {
        description = "Compile-only dependencies for the Hytale plugin '${this@Plugin.name}'."
      }

  override val compileClasspathConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.resolvable("${name}CompileClasspath")

  override fun sourceSet(sourceSet: SourceSet) {
    runtimeOnlyConfiguration.configure {
      extendsFrom(project.configurations.named(sourceSet.runtimeOnlyConfigurationName))
    }

    compileOnlyConfiguration.configure {
      extendsFrom(project.configurations.named(sourceSet.compileOnlyConfigurationName))
    }
  }

  override fun sourceSet(sourceSet: Provider<SourceSet>) = sourceSet(sourceSet.get())

  override val dependencies: DependencyHandler =
      project.objects.newInstance<DependencyHandlerImpl>(this)

  override fun dependencies(configure: Action<in DependencyHandler>) {
    configure.execute(dependencies)
  }
}
