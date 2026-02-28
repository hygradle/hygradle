package dev.hygradle.internal.subsystem

import dev.hygradle.internal.service.auth.AuthService
import org.gradle.api.Plugin
import org.gradle.api.Project

class ServicePlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        gradle.sharedServices.registerIfAbsent("hygradle_auth", AuthService::class.java)
      }
}
