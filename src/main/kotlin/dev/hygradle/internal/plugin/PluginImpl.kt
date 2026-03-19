@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.plugin

import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.dsl.plugin.PluginDependencies
import dev.hygradle.internal.HygradleAttributes
import dev.hygradle.internal.HygradleVariant
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.newInstance

abstract class PluginImpl(private val name: String) : Plugin {

  @get:Inject abstract val project: Project

  override fun getName(): String = name

  val compileOnlyConfiguration =
      project.configurations.dependencyScope("${name}CompileOnly") {
        description = "Compile-only dependencies for plugin '${this@PluginImpl.name}'."
      }

  val compileClasspathConfiguration =
      project.configurations.resolvable("${name}CompileClasspath") {
        description = "Compile classpath for plugin '${this@PluginImpl.name}'."
        extendsFrom(compileOnlyConfiguration)
        extendsFrom(pluginConfiguration)
        extendsFrom(optionalPluginConfiguration)
        attributes {
          attribute(HygradleAttributes.VARIANT_ATTRIBUTE, HygradleVariant.COMPILE)
          attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_API))
          attribute(
              LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
              project.objects.named(LibraryElements.CLASSES),
          )
          attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
        }
      }

  val pluginConfiguration =
      project.configurations.dependencyScope("${name}Plugin") {
        description =
            "Plugin dependencies (compile + runtime) for plugin '${this@PluginImpl.name}'."
      }

  val optionalPluginConfiguration =
      project.configurations.dependencyScope("${name}OptionalPlugin") {
        description =
            "Optional plugin dependencies (compile only) for plugin '${this@PluginImpl.name}'."
      }

  val runtimeOnlyConfiguration =
      project.configurations.dependencyScope("${name}RuntimeOnly") {
        description = "Runtime-only dependencies for plugin '${this@PluginImpl.name}'."
      }

  val runtimeClasspathConfiguration =
      project.configurations.resolvable("${name}RuntimeClasspath") {
        description = "Runtime classpath for plugin '${this@PluginImpl.name}'."
        extendsFrom(runtimeOnlyConfiguration)
        extendsFrom(pluginConfiguration)
        attributes {
          attribute(HygradleAttributes.VARIANT_ATTRIBUTE, HygradleVariant.RUNTIME)
          attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
          attribute(
              LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
              project.objects.named(LibraryElements.CLASSES),
          )
          attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
        }
      }

  val compileElementsConfiguration =
      project.configurations.consumable("${name}CompileElements") {
        description = "Compile elements (classes directories) for plugin '${this@PluginImpl.name}'."
        extendsFrom(compileOnlyConfiguration)
        extendsFrom(pluginConfiguration)
        extendsFrom(optionalPluginConfiguration)
        attributes {
          attribute(HygradleAttributes.VARIANT_ATTRIBUTE, HygradleVariant.COMPILE)
          attribute(HygradleAttributes.PLUGIN_NAME_ATTRIBUTE, this@PluginImpl.name)
          attribute(HygradleAttributes.PLUGIN_BUNDLE_ATTRIBUTE, false)
          attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_API))
          attribute(
              LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
              project.objects.named(LibraryElements.CLASSES),
          )
          attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
        }
      }

  val runtimeElementsConfiguration =
      project.configurations.consumable("${name}RuntimeElements") {
        description = "Runtime elements (classes directories) for plugin '${this@PluginImpl.name}'."
        extendsFrom(runtimeOnlyConfiguration)
        extendsFrom(pluginConfiguration)
        attributes {
          attribute(HygradleAttributes.VARIANT_ATTRIBUTE, HygradleVariant.RUNTIME)
          attribute(HygradleAttributes.PLUGIN_NAME_ATTRIBUTE, this@PluginImpl.name)
          attribute(HygradleAttributes.PLUGIN_BUNDLE_ATTRIBUTE, false)
          attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
          attribute(
              LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
              project.objects.named(LibraryElements.CLASSES),
          )
          attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
        }
      }

  override val dependencies: PluginDependencies =
      project.objects.newInstance<PluginDependenciesImpl>(this)

  override fun dependencies(configure: Action<in PluginDependencies>) =
      configure.execute(dependencies)

  override fun sourceSet(sourceSet: Provider<SourceSet>) =
      this.sourceSetName.set(sourceSet.map { it.name })

  override fun sourceSet(sourceSet: SourceSet) = sourceSet(project.provider { sourceSet })

  init {
    sourceSet(project.sourceSets().named(SourceSet.MAIN_SOURCE_SET_NAME))

    compileOnlyConfiguration.configure {
      extendsFrom(
          sourceSetName
              .flatMap { project.sourceSets().named(it) }
              .flatMap { project.configurations.named(it.compileOnlyConfigurationName) }
      )
    }

    runtimeOnlyConfiguration.configure {
      extendsFrom(
          sourceSetName
              .flatMap { project.sourceSets().named(it) }
              .flatMap { project.configurations.named(it.runtimeOnlyConfigurationName) }
      )
    }

    val wireClassesDirs:
        org.gradle.api.NamedDomainObjectProvider<ConsumableConfiguration>.() -> Unit =
        {
          configure {
            val sourceSet = project.sourceSets().getByName(sourceSetName.get())
            sourceSet.output.classesDirs.files.forEach { classesDir ->
              outgoing.artifact(classesDir) { builtBy(sourceSet.output) }
            }
          }
        }

    compileElementsConfiguration.wireClassesDirs()
    runtimeElementsConfiguration.wireClassesDirs()
  }
}

internal fun Project.sourceSets() = project.extensions.findByType<SourceSetContainer>()!!
