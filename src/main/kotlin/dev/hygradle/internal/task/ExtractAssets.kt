package dev.hygradle.internal.task

import dev.hygradle.dsl.hytale.Patchline
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ExtractAssets
@Inject
constructor(
    private val fileSystemOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) : DefaultTask() {
  @get:Input abstract val version: Property<String>

  @get:Input abstract val patchline: Property<Patchline>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val bundleCache: ConfigurableFileCollection

  @get:OutputDirectory abstract val assetCache: DirectoryProperty

  init {
    assetCache.convention(
        project.layout.projectDirectory.dir(".gradle").dir("caches").dir("hygradle").dir("assets")
    )
  }

  @TaskAction
  fun extract() {
    val fileName = "${patchline.get().toString().lowercase()}-${version.get()}.zip"
    val existingAssets = assetCache.asFileTree.matching { include(fileName) }.singleFile

    // TODO: Also naive operation avoidance here, maybe replace later once Hygradle is more stable
    if (existingAssets.exists()) {
      println(
          "Assets found for ${patchline.get()} version ${version.get()}, skipping extraction..."
      )
      return
    }

    println("Extracting asset bundle...")
    fileSystemOperations.copy {
      from(
          archiveOperations
              .zipTree(bundleCache.asFileTree.matching { include(fileName) }.singleFile)
              .matching { include("Assets.zip") }
              .singleFile
      )
      rename { fileName }
      into(assetCache)
    }
  }
}
