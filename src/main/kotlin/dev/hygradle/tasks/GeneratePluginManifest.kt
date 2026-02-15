package dev.hygradle.tasks

import dev.hygradle.dsl.plugin.manifest.Manifest
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public abstract class GeneratePluginManifest : DefaultTask() {
  @get:Nested public abstract val spec: Manifest

  @get:OutputFile public abstract val manifest: RegularFileProperty

  init {

    val pluginBuildDir =
        spec.name.flatMap { name -> project.layout.buildDirectory.dir("hygradle/plugins/$name") }

    manifest.convention(pluginBuildDir.map { dir -> dir.file("manifest.json") })
  }

  @TaskAction
  public fun generate() {
    manifest
        .get()
        .asFile
        .writeText(
            """
            ${spec.name.get()}
            """
                .trimIndent()
        )
  }
}
