package dev.hygradle.internal.subsystem

import dev.hygradle.internal.service.hytale.HytaleAccount
import org.gradle.api.Plugin
import org.gradle.api.Project

class ServicePlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        gradle.sharedServices.registerIfAbsent(
            "hytale-account",
            HytaleAccount::class.java,
        ) {
          parameters {
            oauthBaseUrl.convention(
                project.providers
                    .gradleProperty(HytaleAccount.OAUTH_BASE_PROPERTY)
                    .orElse(HytaleAccount.OAUTH_BASE)
            )

            accountBaseUrl.convention(
                project.providers
                    .gradleProperty(HytaleAccount.ACCOUNT_BASE_PROPERTY)
                    .orElse(HytaleAccount.ACCOUNT_BASE)
            )

            sessionBaseUrl.convention(
                project.providers
                    .gradleProperty(HytaleAccount.SESSION_BASE_PROPERTY)
                    .orElse(HytaleAccount.SESSION_BASE)
            )

            tokenFile.set(
                project.rootProject.layout.buildDirectory.dir("hygradle/auth").map {
                  it.file("auth.json")
                }
            )
          }
        }
      }
}
