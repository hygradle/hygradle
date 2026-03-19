package dev.hygradle.internal.extension

import dev.hygradle.internal.task.DownloadAssets
import dev.hygradle.internal.task.ExtractAssets
import dev.hygradle.internal.task.GenerateSources
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskProvider

class GlobalTaskRegistry {
  lateinit var downloadAssets: TaskProvider<DownloadAssets>

  lateinit var extractAssets: TaskProvider<ExtractAssets>

  var generateSources: TaskProvider<GenerateSources>? = null
}

internal fun Gradle.globalTaskRegistry(): GlobalTaskRegistry =
    extensions.getByType(GlobalTaskRegistry::class.java)
