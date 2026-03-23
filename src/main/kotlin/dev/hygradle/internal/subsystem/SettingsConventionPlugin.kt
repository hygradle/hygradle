@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal.subsystem

import dev.hygradle.internal.extension.HygradleConfigurations
import dev.hygradle.internal.extension.hygradle
import dev.hygradle.internal.service.settings.settingsService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.create

class SettingsConventionPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        val hygradleSettings = settingsService()

        val ext =
            (hygradle() as ExtensionAware)
                .extensions
                .create<HygradleConfigurations>("configurations")

        ext.hytaleOnly.configure {
          dependencies.addLater(
              hygradleSettings.hytaleVersion.map {
                dependencyFactory.create("com.hypixel.hytale:Server:$it")
              }
          )
        }

        ext.hotswapAgentOnly.configure {
          dependencies.addLater(
              hygradleSettings.hotswapAgentVersion.map {
                dependencyFactory.create("org.hotswapagent:hotswap-agent-core:$it")
              }
          )
        }

        ext.harnessOnly.configure {
          dependencies.addLater(
              hygradleSettings.harnessVersion.map {
                dependencyFactory.create("dev.hygradle:harness:$it")
              }
          )
        }

        ext.hytaleAssetsOnly.configure {
          dependencies.add(project.dependencies.project(mapOf("path" to ":")))
        }
      }
}
