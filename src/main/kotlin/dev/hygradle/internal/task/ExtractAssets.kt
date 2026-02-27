package dev.hygradle.internal.task

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

@CacheableTask
abstract class ExtractAssets : Copy() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val assetBundle: RegularFileProperty

  @get:OutputDirectory abstract val cacheDirectory: DirectoryProperty

  @get:OutputFile abstract val assets: RegularFileProperty

  init {
    cacheDirectory.convention(
        project.layout.projectDirectory.dir(".gradle").dir("caches").dir("hygradle").dir("assets")
    )

    assets.convention(
        cacheDirectory.zip(assetBundle) { dir, bundle -> dir.file(bundle.asFile.name) }
    )

    from(
        assetBundle
            .map { project.zipTree(it) }
            .map { it.matching { include("Assets.zip") }.singleFile }
    )

    into(cacheDirectory)
  }

  override fun copy() {
    rename { assets.get().asFile.name }
    println("Extracting assets...")
    super.copy()
  }
}
