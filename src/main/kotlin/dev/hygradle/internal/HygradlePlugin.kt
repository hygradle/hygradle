@file:Suppress("Unused", "UnstableApiUsage")

package dev.hygradle.internal

import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.dsl.run.Run
import dev.hygradle.internal.artifact.transform.Decompile
import dev.hygradle.internal.extension.HygradleExtension
import dev.hygradle.tasks.ExecuteServerRun
import dev.hygradle.tasks.GeneratePluginManifest
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.PluginAware
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerTransform
import org.gradle.kotlin.dsl.withType

class HygradlePlugin : Plugin<PluginAware> {
  override fun apply(target: PluginAware): Unit =
      when (target) {
        is Project -> apply(target)
        else -> throw GradleException("Hygradle must be applied at the project level.")
      }

  private fun apply(project: Project) {
    val ext = project.extensions.create<HygradleExtension>("hygradle")

    with(project.plugins) {
      apply(ConventionPlugin::class.java)
      apply(RepositoryPlugin::class.java)
    }

    val decompiled = Attribute.of("decompiled", Boolean::class.javaObjectType)

    project.dependencies.attributesSchema.attribute(decompiled)
    project.dependencies.registerTransform(Decompile::class) {
      from.attribute(decompiled, false)
      to.attribute(decompiled, true)
    }
    project.dependencies.artifactTypes.named(ArtifactTypeDefinition.JAR_TYPE) {
      attributes.attribute(decompiled, false)
    }

    ext.hytale.hytaleClasspath.configure { attributes.attribute(decompiled, true) }

    ext.plugins.withType<dev.hygradle.dsl.plugin.Plugin>().all {
      project.configurations.named(sourceSetCompileOnlyConfigurationName.get()).configure {
        extendsFrom(ext.hytale.hytaleOnly)
      }
    }

    ext.plugins.withType<LatePlugin>().all {
      val generateManifest =
          project.tasks.register<GeneratePluginManifest>("${name}GenerateManifest") {
            group = "hygradle/plugins/${this@all.name}"
            spec.set(this@all.manifest)
          }
    }

    ext.runs.withType<Run>().all {
      val executeRun =
          project.tasks.register<ExecuteServerRun>("${name}Run") {
            group = "hygradle/runs/${this@all.name}"
            ext.plugins.all {
              classpathProvider.from(this.runtimeClasspathConfiguration, ext.hytale.hytaleClasspath)
            }
          }
    }
  }
}
