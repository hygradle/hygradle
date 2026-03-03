package dev.hygradle.internal.subsystem

import dev.hygradle.internal.service.auth.AccessManager
import org.gradle.api.Plugin
import org.gradle.api.Project

class ServicePlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        gradle.sharedServices.registerIfAbsent("hygradle-access", AccessManager::class.java)
      }
}
