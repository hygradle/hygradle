@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.run

import dev.hygradle.dsl.run.Run
import dev.hygradle.dsl.run.RunDependencies
import dev.hygradle.internal.attributes.Category as HygradleCategory
import dev.hygradle.internal.attributes.Usage as HygradleUsage
import java.util.Locale.getDefault
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class RunImpl(private val name: String) : Run {

  @get:Inject abstract val project: Project

  @get:Inject abstract val objects: ObjectFactory

  val taskGroup = "hygradle/runs/$name"

  val taskSlug = name.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
  }

  override fun getName(): String = name

  val runtimeOnly =
      project.configurations.dependencyScope("_hygradle_run_${name}RuntimeOnly") {
        description = "Runtime-only dependencies for run '${this@RunImpl.name}'."
      }

  val runtimeClasspath =
      project.configurations.resolvable("_hygradle_run_${name}RuntimeClasspath") {
        description = "Runtime classpath for run '${this@RunImpl.name}'."
        extendsFrom(runtimeOnly)

        attributes {
          attribute(
              Usage.USAGE_ATTRIBUTE,
              objects.named(Usage::class.java, HygradleUsage.RUNTIME),
          )

          attribute(
              Category.CATEGORY_ATTRIBUTE,
              objects.named(Category::class.java, HygradleCategory.HYGRADLE),
          )
        }
      }

  override val dependencies: RunDependencies = objects.newInstance<RunDependenciesImpl>(this)

  override fun dependencies(configure: Action<in RunDependencies>) = configure.execute(dependencies)
}
