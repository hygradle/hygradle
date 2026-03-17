package dev.hygradle.internal.task.plugin

import dev.hygradle.dsl.plugin.manifest.Manifest
import dev.hygradle.internal.plugin.manifest.SerializableManifest
import dev.hygradle.internal.plugin.manifest.toSerializable
import javax.inject.Inject
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateManifest : DefaultTask() {
  @get:Inject abstract val layout: ProjectLayout

  @get:Nested abstract val manifest: Property<Manifest>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.NONE)
  @get:Optional
  abstract val dependencyManifests: ConfigurableFileCollection

  @get:OutputDirectory abstract val manifestDirectory: DirectoryProperty

  @get:Internal val manifestFile: Provider<RegularFile> = manifestDirectory.file("manifest.json")

  init {
    manifestDirectory.convention(
        manifest.flatMap {
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

    val serializable = manifest.get().toSerializable()
    val merged = mergeDiscoveredDependencies(serializable, json)
    manifestFile.get().asFile.writeText(json.encodeToString(merged))
  }

  private fun mergeDiscoveredDependencies(
      base: SerializableManifest,
      json: Json,
  ): SerializableManifest {
    if (dependencyManifests.isEmpty) return base

    val userDeclaredKeys = buildSet {
      base.dependencies?.keys?.let(::addAll)
      base.optionalDependencies?.keys?.let(::addAll)
    }

    val discovered = buildMap {
      for (file in dependencyManifests.files) {
        val dep = json.decodeFromString<SerializableManifest>(file.readText())
        val key = "${dep.group}:${dep.name}"
        if (key !in userDeclaredKeys) put(key, dep.version)
      }
    }

    if (discovered.isEmpty()) return base

    val mergedDeps = buildMap {
      putAll(discovered)
      base.dependencies?.let(::putAll)
    }

    return base.copy(dependencies = mergedDeps.ifEmpty { null })
  }
}
