package dev.hygradle.tasks

import dev.hygradle.dsl.hytale.Patchline
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

@CacheableTask
abstract class ExtractAssetBundle : Copy() {
  @get:Input abstract val patchline: Property<Patchline>
  @get:Input abstract val version: Property<String>

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val cacheDirectory: DirectoryProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val gameBundle: RegularFileProperty

  @get:OutputFile abstract val assetBundle: RegularFileProperty

  init {
    cacheDirectory.convention(
        project.layout.projectDirectory.dir(".gradle").dir("caches").dir("hygradle").dir("assets")
    )

    assetBundle.set(
        cacheDirectory.file(
            patchline.zip(
                version,
            ) { patchline, version ->
              "${patchline.toString().lowercase()}-${version}.zip"
            }
        )
    )

    from(
        gameBundle
            .map { project.zipTree(it) }
            .map { it.matching { include("Assets.zip") }.singleFile }
    )

    into(cacheDirectory)
  }

  override fun copy() {
    rename { assetBundle.get().asFile.name }
    super.copy()
  }
}
