package dev.hygradle.internal.task

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Content-hashing the assets is very time-consuming.")
abstract class ExtractAssets : DefaultTask() {
  @get:Inject abstract val fs: FileSystemOperations

  @get:Inject abstract val archives: ArchiveOperations

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val assetBundle: ConfigurableFileCollection

  @get:OutputDirectory abstract val assetCacheDirectory: DirectoryProperty

  @TaskAction
  fun extract() {
    val assetBundle = assetBundle.singleFile
    val cacheDir = assetCacheDirectory.get()
    val assets = cacheDir.file(assetBundle.name).asFile

    cacheDir.asFileTree.visit { if (file != assets) file.delete() }

    // TODO: Finer caching that doesn't require content hashing 4 gigs?
    if (assets.exists()) return

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
