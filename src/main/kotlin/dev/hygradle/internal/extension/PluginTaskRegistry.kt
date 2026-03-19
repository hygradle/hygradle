package dev.hygradle.internal.extension

import dev.hygradle.internal.task.plugin.AssembleAssets
import dev.hygradle.internal.task.plugin.GenerateManifest
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider

class PluginTaskRegistry {
  val manifestTasks = mutableMapOf<String, TaskProvider<GenerateManifest>>()
  val assetTasks = mutableMapOf<String, TaskProvider<AssembleAssets>>()
}

internal fun Project.pluginTaskRegistry(): PluginTaskRegistry =
    (hygradle() as ExtensionAware).extensions.getByType(PluginTaskRegistry::class.java)
