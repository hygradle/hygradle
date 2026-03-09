package dev.hygradle.internal.subsystem

import dev.hygradle.internal.service.HytaleAccountService
import dev.hygradle.internal.service.hytale.HytaleAccount
import org.gradle.api.Plugin
import org.gradle.api.Project

class ServicePlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        gradle.sharedServices.registerIfAbsent(
            "hygradle-account",
            HytaleAccountService::class.java,
        ) {
          this.parameters.tokenFile.set(
              project.rootProject.layout.buildDirectory.dir("hygradle/auth").map {
                it.file("auth.json")
              }
          )
        }

        gradle.sharedServices.registerIfAbsent(
            "hytale-account",
            HytaleAccount::class.java,
        ) {
          this.parameters.tokenFile.set(
              project.rootProject.layout.buildDirectory.dir("hygradle/auth").map {
                it.file("auth.json")
              }
          )
        }
      }
}
