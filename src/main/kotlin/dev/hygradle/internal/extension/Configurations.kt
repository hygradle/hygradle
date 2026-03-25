@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.extension

import dev.hygradle.internal.attributes.Category as HygradleCategory
import dev.hygradle.internal.attributes.Usage as HygradleUsage
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.named

abstract class Configurations : ExtensionAware {

  @get:Inject abstract val project: Project

  val hytaleOnly = project.configurations.dependencyScope("hytaleOnly")

  val hytaleClasspath =
      project.configurations.resolvable("hytaleClasspath") { extendsFrom(hytaleOnly) }

  val harnessOnly = project.configurations.dependencyScope("harnessOnly")

  val harnessClasspath =
      project.configurations.resolvable("harnessClasspath") { extendsFrom(harnessOnly) }

  val hotswapAgentOnly = project.configurations.dependencyScope("hotswapAgentOnly")

  val hotswapAgentClasspath =
      project.configurations.resolvable("hotswapAgentClasspath") { extendsFrom(hotswapAgentOnly) }

  val hytaleAssetsOnly = project.configurations.dependencyScope("_hygradle_hytaleAssetsOnly")

  val hytaleAssetsClasspath =
      project.configurations.resolvable("_hygradle_hytaleAssetsClasspath") {
        extendsFrom(hytaleAssetsOnly)

        attributes {
          attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(HygradleUsage.RUNTIME))

          attribute(
              Category.CATEGORY_ATTRIBUTE,
              project.objects.named(HygradleCategory.HYTALE_ASSETS),
          )
        }
      }
}

internal fun Project.hygradleConfigurations(): Configurations =
    hygradle().extensions.getByType(Configurations::class.java)
