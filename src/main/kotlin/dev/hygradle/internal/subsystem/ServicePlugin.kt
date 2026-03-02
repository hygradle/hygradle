package dev.hygradle.internal.subsystem

import dev.hygradle.internal.service.auth.AuthManager
import org.gradle.api.Plugin
import org.gradle.api.Project

class ServicePlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        gradle.sharedServices.registerIfAbsent("hygradle-auth", AuthManager::class.java) {
          parameters {
            projectName.set(rootProject.name)
            authFile.set(
                project.layout.buildDirectory.dir("hygradle/auth").map { it.file("auth.enc") }
            )
          }
        }
      }
}
