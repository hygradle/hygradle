@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.plugin

import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.dsl.plugin.PluginDependencies
import dev.hygradle.internal.attributes.Category as HygradleCategory
import dev.hygradle.internal.attributes.Usage as HygradleUsage
import java.util.Locale.getDefault
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.newInstance

abstract class PluginImpl(private val name: String) : Plugin {

  @get:Inject abstract val project: Project

  override fun getName(): String = name

  val taskGroup = "hygradle/plugins/$name"

  val taskSlug = name.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
  }

  val compileOnly =
      project.configurations.dependencyScope("_hygradle_${name}CompileOnly") {
        description = "Compile-only dependencies for plugin '${this@PluginImpl.name}'."
      }

  val compileClasspath =
      project.configurations.resolvable("_hygradle_${name}CompileClasspath") {
        description = "Compile classpath for plugin '${this@PluginImpl.name}'."
        extendsFrom(compileOnly)
      }

  val compileElements =
      project.configurations.consumable("_hygradle_${name}CompileElements") {
        extendsFrom(compileOnly)

        // Consumers don't request a version, so the version here doesn't matter
        outgoing.capability("dev.hygradle.plugin:${this@PluginImpl.name}:0.0.0")

        addPluginClassDirectories()

        attributes {
          attribute(
              Usage.USAGE_ATTRIBUTE,
              project.objects.named(Usage::class.java, HygradleUsage.COMPILE),
          )

          attribute(
              Category.CATEGORY_ATTRIBUTE,
              project.objects.named(Category::class.java, HygradleCategory.HYGRADLE),
          )
        }
      }

  val runtimeOnly =
      project.configurations.dependencyScope("_hygradle_${name}RuntimeOnly") {
        description = "Runtime-only dependencies for plugin '${this@PluginImpl.name}'."
      }

  val runtimeClasspath =
      project.configurations.resolvable("_hygradle_${name}RuntimeClasspath") {
        description = "Runtime classpath for plugin '${this@PluginImpl.name}'."
        extendsFrom(runtimeOnly)

        attributes {
          attribute(
              Usage.USAGE_ATTRIBUTE,
              project.objects.named(Usage::class.java, HygradleUsage.RUNTIME),
          )

          attribute(
              Category.CATEGORY_ATTRIBUTE,
              project.objects.named(Category::class.java, HygradleCategory.HYGRADLE),
          )
        }
      }

  val runtimeElements =
      project.configurations.consumable("_hygradle_${name}RuntimeElements") {
        extendsFrom(runtimeOnly)

        // Consumers don't request a version, so the version here doesn't matter
        outgoing.capability("dev.hygradle.plugin:${this@PluginImpl.name}:0.0.0")

        addPluginClassDirectories()

        attributes {
          attribute(
              Usage.USAGE_ATTRIBUTE,
              project.objects.named(Usage::class.java, HygradleUsage.RUNTIME),
          )

          attribute(
              Category.CATEGORY_ATTRIBUTE,
              project.objects.named(Category::class.java, HygradleCategory.HYGRADLE),
          )
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

    compileOnly.configure {
      extendsFrom(
          sourceSetName
              .flatMap { project.sourceSets().named(it) }
              .flatMap { project.configurations.named(it.compileOnlyConfigurationName) }
      )
    }

    runtimeOnly.configure {
      extendsFrom(
          sourceSetName
              .flatMap { project.sourceSets().named(it) }
              .flatMap { project.configurations.named(it.runtimeOnlyConfigurationName) }
      )
    }
  }

  private fun ConsumableConfiguration.addPluginClassDirectories() {
    val sourceSet = project.sourceSets().getByName(sourceSetName.get())

    for (dir in sourceSet.output.classesDirs.files) {
      outgoing.artifact(dir) { builtBy(sourceSet.output) }
    }
  }
}

internal fun Project.sourceSets() = project.extensions.findByType<SourceSetContainer>()!!
