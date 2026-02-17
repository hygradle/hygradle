@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.plugin

import dev.hygradle.dsl.plugin.DependencyHandler
import dev.hygradle.dsl.plugin.Plugin
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.newInstance

abstract class PluginImpl @Inject internal constructor(private val name: String, project: Project) :
    Plugin {
  override fun getName(): String = name

  override val compileOnlyConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.dependencyScope("${name}CompileOnly") {
        description = "Compile-only dependencies for plugin '${this@PluginImpl.name}'."
      }

  override val compileClasspathConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.resolvable("${name}CompileClasspath") {
        description = "Compile classpath for plugin '${this@PluginImpl.name}'."
      }

  override val runtimeOnlyConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.dependencyScope("${name}RuntimeOnly") {
        description = "Runtime-only dependencies for plugin '${this@PluginImpl.name}'."
      }

  override val runtimeClasspathConfiguration: NamedDomainObjectProvider<out Configuration> =
      project.configurations.resolvable("${name}RuntimeClasspath") {
        description = "Runtime classpath for plugin '${this@PluginImpl.name}'."
      }

  override val dependencies: DependencyHandler =
      project.objects.newInstance<DependencyHandlerImpl>(this)

  override fun dependencies(configure: Action<in DependencyHandler>) =
      configure.execute(dependencies)

  override fun sourceSet(sourceSet: Provider<SourceSet>) = sourceSet(sourceSet.get())

  override fun sourceSet(sourceSet: SourceSet) {
    sourceSetCompileOnlyConfigurationName.set(sourceSet.compileOnlyConfigurationName)
    sourceSetRuntimeOnlyConfigurationName.set(sourceSet.runtimeOnlyConfigurationName)
  }

  init {
    sourceSet(
        project.extensions.findByType<SourceSetContainer>()!!.named(SourceSet.MAIN_SOURCE_SET_NAME)
    )

    compileOnlyConfiguration.configure {
      extendsFrom(
          sourceSetCompileOnlyConfigurationName.flatMap { project.configurations.named(it) }
      )
    }

    compileClasspathConfiguration.configure { extendsFrom(compileOnlyConfiguration) }

    runtimeOnlyConfiguration.configure {
      extendsFrom(
          sourceSetRuntimeOnlyConfigurationName.flatMap { project.configurations.named(it) }
      )
    }

    runtimeClasspathConfiguration.configure { extendsFrom(runtimeOnlyConfiguration) }
  }
}
