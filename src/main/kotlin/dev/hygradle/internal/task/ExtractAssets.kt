package dev.hygradle.internal.task

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ExtractAssets : DefaultTask() {
  @get:Inject abstract val fs: FileSystemOperations

  @get:Inject abstract val archives: ArchiveOperations

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val assetBundle: ConfigurableFileCollection

  @get:OutputDirectory abstract val assetCacheDirectory: DirectoryProperty

  init {
    assetCacheDirectory.convention(
        project.layout.projectDirectory.dir(".gradle/caches/hygradle/assets")
    )
  }

  @TaskAction
  fun extract() {
    val assetBundle = assetBundle.singleFile
    val cacheDir = assetCacheDirectory.get()
    val assets = cacheDir.file(assetBundle.name).asFile

    cacheDir.asFileTree.visit { if (file != assets) file.delete() }

    println(assets)
    println(assets.exists())

    if (assets.exists()) return
    else {
      fs.copy {
        from(archives.zipTree(assetBundle).matching { include(ASSET_BUNDLE_NAME) }.singleFile)
        into(cacheDir)
        rename { assets.name }
      }
    }
  }

  companion object {
    const val ASSET_BUNDLE_NAME = "Assets.zip"
  }
}
