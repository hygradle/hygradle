package dev.hygradle.tasks

import dev.hygradle.dsl.plugin.manifest.PluginManifest
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public abstract class GeneratePluginManifest : DefaultTask() {
  @get:Nested public abstract val spec: Property<PluginManifest>

  @get:OutputFile public abstract val manifest: RegularFileProperty

  @TaskAction public fun generate() {}
}
