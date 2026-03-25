@file:Suppress("UnstableApiUsage", "Unused")

package dev.hygradle.internal

import dev.hygradle.dsl.extension.Repository
import dev.hygradle.dsl.settings.HygradleSettings
import dev.hygradle.internal.extension.RepositoryExtension
import dev.hygradle.internal.service.hytale.HytaleAccount
import dev.hygradle.internal.service.settings.HygradleSettingsService
import dev.hygradle.internal.settings.HygradleSettingsImpl
import dev.hygradle.internal.settings.hygradle
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.newInstance

abstract class HygradleSettingsPlugin : Plugin<Settings> {

  @get:Inject abstract val objects: ObjectFactory

  override fun apply(settings: Settings) =
      with(settings) {
        extensions.add(
            HygradleSettings::class.java,
            "hygradle",
            objects.newInstance<HygradleSettingsImpl>(),
        )

        registerServices()

        (dependencyResolutionManagement.repositories as ExtensionAware)
            .extensions
            .add(
                Repository::class.java,
                "hygradle",
                objects.newInstance<RepositoryExtension>(
                    dependencyResolutionManagement.repositories,
                    hygradle().hytale.patchline,
                ),
            )

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
        vineflowerVersion.set(settings.hygradle().vineflower.version)
      }
    }
  }
}
