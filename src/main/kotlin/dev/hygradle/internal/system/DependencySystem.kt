package dev.hygradle.internal.system

import dev.hygradle.dsl.extension.Repository
import dev.hygradle.internal.extension.Configurations
import dev.hygradle.internal.extension.RepositoryExtension
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.service.settings.settingsService
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.newInstance

class DependencySystem : Plugin<Project> {
  override fun apply(project: Project) =
      with(project) {
        val configs =
            (hygradle() as ExtensionAware).extensions.create<Configurations>("_configurations")

        configs.hytaleOnly.addDependencyFromSettings(
            settingsService().hytaleVersion,
            "com.hypixel.hytale:Server",
        )

        configs.hotswapAgentOnly.addDependencyFromSettings(
            settingsService().hotswapAgentVersion,
            "org.hotswapagent:hotswap-agent-core",
        )

        configs.harnessOnly.addDependencyFromSettings(
            settingsService().harnessVersion,
            "dev.hygradle:harness",
        )

        configs.hytaleAssetsOnly.configure {
          dependencies.add(project.dependencyFactory.create(project.rootProject))
        }

        (project.repositories as ExtensionAware)
            .extensions
            .add(
                Repository::class.java,
                "hygradle",
                objects.newInstance<RepositoryExtension>(
                    project.repositories,
                    settingsService().hytalePatchline,
                    gradle.gradleUserHomeDir,
                ),
            )
      }

  context(project: Project)
  private fun NamedDomainObjectProvider<out Configuration>.addDependencyFromSettings(
      provider: Provider<String>,
      groupArtifact: String,
  ) = configure {
    dependencies.addLater(provider.map { project.dependencyFactory.create("$groupArtifact:$it") })
  }
}
