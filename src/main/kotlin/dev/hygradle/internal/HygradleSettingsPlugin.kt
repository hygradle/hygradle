package dev.hygradle.internal

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.toolchains.foojay.FoojayToolchainsPlugin

class HygradleSettingsPlugin : Plugin<Settings> {
  override fun apply(settings: Settings) {
    settings.plugins.apply(RepositoryPlugin::class.java)
    settings.plugins.apply(FoojayToolchainsPlugin::class.java)
  }
}
