package dev.hygradle.internal.task

import dev.hygradle.internal.service.HytaleAuth
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.TaskAction

abstract class DownloadGameAssets : DefaultTask() {
  @get:ServiceReference abstract val hytaleAuth: Property<HytaleAuth>

  @TaskAction
  fun downloadAssets() {
    runBlocking { hytaleAuth.get().startBrowserFlow() }
  }
}
