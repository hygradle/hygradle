package dev.hygradle.internal.task.plugin

import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.internal.plugin.manifest.toSerializable
import javax.inject.Inject
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateManifest : DefaultTask() {
  @get:Inject abstract val layout: ProjectLayout

  @get:Nested abstract val spec: Property<Manifest>

  @get:OutputDirectory abstract val manifestDirectory: DirectoryProperty

  @get:Internal val manifest: Provider<RegularFile> = manifestDirectory.file("manifest.json")

  init {
    manifestDirectory.convention(
        spec.flatMap {
          it.name.flatMap { name -> layout.buildDirectory.dir("hygradle/plugins/$name/manifest") }
        }
    )
  }

  @TaskAction
  fun generate() {
    val json = Json {
      prettyPrint = true
      explicitNulls = false
    }

    manifest.get().asFile.writeText(json.encodeToString(spec.get().toSerializable()))
  }
}
