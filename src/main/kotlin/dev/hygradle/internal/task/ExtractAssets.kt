package dev.hygradle.internal.task

import dev.hygradle.dsl.hytale.Patchline
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Content-hashing the assets is very time-consuming.")
abstract class ExtractAssets : DefaultTask() {
  @get:Inject abstract val fs: FileSystemOperations

  @get:Inject abstract val archives: ArchiveOperations

  @get:Input abstract val version: Property<String>

  @get:Input abstract val patchline: Property<Patchline>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val assetBundle: ConfigurableFileCollection

  @get:OutputDirectory abstract val assetCacheDirectory: DirectoryProperty

  @get:Internal abstract val assetsFile: RegularFileProperty

  init {
    assetsFile.convention(
        assetCacheDirectory.zip(
            patchline.zip(version) { patchline, version -> "$patchline-$version.zip" }
        ) { dir, filename ->
          dir.file(filename)
        }
    )
  }

  @TaskAction
  fun extract() {
    val assetBundle = assetBundle.singleFile
    val cacheDir = assetCacheDirectory.get()
    val assets = assetsFile.get().asFile

    cacheDir.asFileTree.visit { if (file != assets) file.delete() }

    if (assets.exists() && assets.isValidZip()) {
      return
    }

    fs.copy {
      from(archives.zipTree(assetBundle).matching { include(ASSET_BUNDLE_NAME) }.singleFile)
      into(cacheDir)
      rename { assets.name }
    }
  }

  companion object {
    const val ASSET_BUNDLE_NAME = "Assets.zip"
  }
}
