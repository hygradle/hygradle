package dev.hygradle.tasks

import dev.hygradle.dsl.plugin.manifest.Manifest
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GeneratePluginManifest : DefaultTask() {
  @get:Nested abstract val spec: Property<Manifest>

  @get:OutputFile abstract val manifest: RegularFileProperty

  init {
    manifest.convention(
        spec
            .flatMap {
              it.name.flatMap { name ->
                project.layout.buildDirectory.dir("hygradle/plugins/$name/manifest")
              }
            }
            .map { dir -> dir.file("manifest.json") }
    )
  }

  @TaskAction
  fun generate() {
    manifest
        .get()
        .asFile
        .writeText(
            """
            {
              "Name": "${spec.get().name.get()}",
              "Group": "test",
              "Main": "${spec.get().mainClass.get()}",
              "ServerVersion": "${spec.get().serverVersion.get()}",
              "IncludesAssetPack": true
            }
            """
                .trimIndent()
        )
  }
}
