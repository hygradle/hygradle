@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.subsystem

import dev.hygradle.dsl.settings.HygradleSettings
import dev.hygradle.internal.extension.HygradleConfigurations
import dev.hygradle.internal.extension.hygradle
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.newInstance

class SettingsConventionPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        val settings =
            gradle.extensions.findByType(HygradleSettings::class.java)
                ?: throw GradleException(
                    "The 'dev.hygradle.settings' plugin must be applied in settings.gradle.kts before using 'dev.hygradle'."
                )

        (hygradle() as ExtensionAware)
            .extensions
            .add(HygradleSettings::class.java, "settings", settings)

        val configurations = objects.newInstance<HygradleConfigurations>()

        (hygradle() as ExtensionAware)
            .extensions
            .add(HygradleConfigurations::class.java, "configurations", configurations)

        dependencies.addProvider(
            configurations.hytaleOnly.name,
            settings.hytale.version.map { dependencies.create("com.hypixel.hytale:Server:$it") },
        )

        if (settings.hytale.decompile.get()) {
          dependencies.add(
              configurations.vineflowerOnly.name,
              "org.vineflower:vineflower:1.11.1",
          )
        }

        dependencies.addProvider(
            configurations.hotswapAgentOnly.name,
            settings.hotswapAgent.version.map {
              dependencies.create("org.hotswapagent:hotswap-agent-core:$it")
            },
        )

        dependencies.addProvider(
            configurations.harnessOnly.name,
            settings.harness.version.map { dependencies.create("dev.hygradle:harness:$it") },
        )
      }
}

internal fun Project.hygradleSettings(): HygradleSettings =
    (hygradle() as ExtensionAware).extensions.getByType<HygradleSettings>()
