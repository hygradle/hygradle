@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.system

import dev.hygradle.internal.attributes.Category as HygradleCategory
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.extension.hygradleConfigurations
import dev.hygradle.internal.run.RunImpl
import dev.hygradle.internal.service.settings.settingsService
import dev.hygradle.internal.task.run.PrepareRunDirectory
import dev.hygradle.internal.task.run.RunHytaleServer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class RunSystem : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) { hygradle().runs.all { configureRun(this as RunImpl) } }

  context(project: Project)
  private fun configureRun(run: RunImpl) {
    val prepareRunDirectory =
        project.tasks.register<PrepareRunDirectory>("prepare${run.taskSlug}RunDirectory") {
          group = run.taskGroup
          runName.set(run.name)
        }

    project.tasks.register<RunHytaleServer>("start${run.taskSlug}Server") {
      group = run.taskGroup

      // TODO: Rip this shit out when the shared server source is available
      if (project.settingsService().hytaleDecompile.get()) {
        dependsOn("${project.rootProject.isolated.path}generateSources")
      }

      runDirectory.set(prepareRunDirectory.flatMap { it.runDirectory })
      classpathProvider.from(project.hygradleConfigurations().hytaleClasspath)

      assets.from(project.hygradleConfigurations().hytaleAssetsClasspath)
      hotswapAgent.from(project.hygradleConfigurations().hotswapAgentClasspath)
      harness.from(project.hygradleConfigurations().harnessClasspath)

      classpathProvider.from(run.runtimeClasspath)

      classpathProvider.from(
          run.runtimeClasspath.map {
            it.incoming
                .artifactView {
                  attributes {
                    attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.objects.named(HygradleCategory.PLUGIN_ASSETS),
                    )
                  }
                  lenient(true)
                }
                .files
          }
      )
    }
  }
}
