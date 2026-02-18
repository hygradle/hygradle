package dev.hygradle.tasks

import dev.hygradle.dsl.plugin.manifest.Manifest
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GeneratePluginManifest : DefaultTask() {
  @get:Nested abstract val spec: Property<Manifest>

  @get:OutputFile abstract val manifest: RegularFileProperty

  init {
    val pluginBuildDir =
        spec.flatMap {
          it.name.flatMap { name -> project.layout.buildDirectory.dir("hygradle/plugins/$name") }
        }

    manifest.convention(pluginBuildDir.map { dir -> dir.file("manifest.json") })
  }

  @TaskAction
  fun generate() {
    manifest
        .get()
        .asFile
        .writeText(
            """
            ${spec.get().name.get()}
            """
                .trimIndent()
        )
  }
}
