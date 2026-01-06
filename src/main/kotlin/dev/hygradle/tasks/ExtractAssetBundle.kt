package dev.hygradle.tasks

import dev.hygradle.internal.HytalePatchline
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile

public abstract class ExtractAssetBundle : Copy() {
  @get:Input public abstract val patchline: Property<HytalePatchline>
  @get:Input public abstract val hytaleVersion: Property<String>
  @get:InputFile public abstract val gameBundle: RegularFileProperty
  @get:OutputFile public abstract val assetBundle: RegularFileProperty

  init {
    assetBundle.convention(project.layout.buildDirectory.file("assets.zip"))
  }

  override fun copy() {
    println("Extracting asset bundle...")

    from(gameBundle)
    into(project.layout.buildDirectory.dir("bundle"))
    super.copy()
  }
}
