package dev.hygradle.internal

import dev.hygradle.dsl.settings.HygradleSettings
import dev.hygradle.internal.settings.HygradleSettingsImpl
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.create
import org.gradle.toolchains.foojay.FoojayToolchainsPlugin

@Suppress("Unused")
class HygradleSettingsPlugin : Plugin<Settings> {
  override fun apply(settings: Settings) {
    settings.plugins.apply(FoojayToolchainsPlugin::class.java)

    val ext = settings.extensions.create<HygradleSettingsImpl>("hygradle")
    settings.gradle.extensions.add(HygradleSettings::class.java, "hygradle", ext)

    settings.gradle.settingsEvaluated { ext.lock() }
  }
}
