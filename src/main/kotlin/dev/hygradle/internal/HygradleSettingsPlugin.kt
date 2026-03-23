@file:Suppress("UnstableApiUsage", "Unused")

package dev.hygradle.internal

import dev.hygradle.internal.service.hytale.HytaleAccount
import dev.hygradle.internal.service.settings.HygradleSettingsService
import dev.hygradle.internal.settings.HygradleSettingsImpl
import dev.hygradle.internal.settings.hygradle
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create

class HygradleSettingsPlugin : Plugin<Settings> {
  override fun apply(settings: Settings) =
      with(settings) {
        extensions.create<HygradleSettingsImpl>("hygradle")
        registerServices()

        settings.gradle.lifecycle.beforeProject {
          if (this.rootProject == this) this.pluginManager.apply(HygradleRootPlugin::class)
        }
      }

  context(settings: Settings)
  private fun registerServices() {
    val cacheDir = settings.gradle.gradleUserHomeDir.resolve("caches/hygradle")

    settings.gradle.sharedServices.registerIfAbsent(
        "hytale-account",
        HytaleAccount::class.java,
    ) {
      parameters {
        oauthBaseUrl.convention(
            settings.providers
                .gradleProperty(HytaleAccount.OAUTH_BASE_PROPERTY)
                .orElse(HytaleAccount.OAUTH_BASE)
        )

        accountBaseUrl.convention(
            settings.providers
                .gradleProperty(HytaleAccount.ACCOUNT_BASE_PROPERTY)
                .orElse(HytaleAccount.ACCOUNT_BASE)
        )

        sessionBaseUrl.convention(
            settings.providers
                .gradleProperty(HytaleAccount.SESSION_BASE_PROPERTY)
                .orElse(HytaleAccount.SESSION_BASE)
        )

        tokenFile.fileValue(cacheDir.resolve("auth/auth.json"))
      }
    }

    // TODO: Not quite sure this is the most idiomatic way of propagating settings, but it is
    // IP-compatible
    settings.gradle.sharedServices.registerIfAbsent(
        "hygradle-settings",
        HygradleSettingsService::class.java,
    ) {
      parameters {
        hytaleVersion.set(settings.hygradle().hytale.version)
        hytalePatchline.set(settings.hygradle().hytale.patchline)
        hytaleDecompile.set(settings.hygradle().hytale.decompile)
        hotswapAgentVersion.set(settings.hygradle().hotswapAgent.version)
        harnessVersion.set(settings.hygradle().harness.version)
      }
    }
  }
}
