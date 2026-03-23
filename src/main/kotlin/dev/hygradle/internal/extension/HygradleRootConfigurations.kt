@file:Suppress("UnstableApiUsage", "Unused")

package dev.hygradle.internal.extension

import dev.hygradle.internal.attributes.Category as HygradleCategory
import dev.hygradle.internal.attributes.Usage as HygradleUsage
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.named

abstract class HygradleRootConfigurations {

  @get:Inject abstract val project: Project

  val hytaleOnly = project.configurations.dependencyScope("_hygradle_hytaleOnly")

  val hytaleClasspath =
      project.configurations.resolvable("_hygradle_hytaleClasspath") { extendsFrom(hytaleOnly) }

  val hytaleAssetsRuntimeElements =
      project.configurations.consumable("_hygradle_hytaleAssetsRuntimeElements") {
        attributes {
          attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(HygradleUsage.RUNTIME))

          attribute(
              Category.CATEGORY_ATTRIBUTE,
              project.objects.named(HygradleCategory.HYTALE_ASSETS),
          )
        }
      }

  val vineflowerOnly = project.configurations.dependencyScope("_hygradle_vineflowerOnly")

  val vineflowerClasspath =
      project.configurations.resolvable("_hygradle_vineflowerClasspath") {
        extendsFrom(vineflowerOnly)
      }
}

internal fun Project.rootHygradleConfigurations() =
    extensions.getByType(HygradleRootConfigurations::class.java)
