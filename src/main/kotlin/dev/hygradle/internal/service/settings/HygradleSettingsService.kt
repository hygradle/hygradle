package dev.hygradle.internal.service.settings

import dev.hygradle.dsl.hytale.Patchline
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class HygradleSettingsService : BuildService<HygradleSettingsService.Parameters> {
  interface Parameters : BuildServiceParameters {
    val hytaleVersion: Property<String>
    val hytalePatchline: Property<Patchline>
    val hytaleDecompile: Property<Boolean>
    val hotswapAgentVersion: Property<String>
    val harnessVersion: Property<String>
    val vineflowerVersion: Property<String>
  }
}

internal fun Project.settingsService() =
    project.gradle.sharedServices.registrations.getByName("hygradle-settings").parameters
        as HygradleSettingsService.Parameters
