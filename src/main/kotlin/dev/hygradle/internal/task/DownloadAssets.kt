package dev.hygradle.internal.task

import dev.hygradle.dsl.hytale.Patchline
import dev.hygradle.internal.service.HytaleAccountService
import dev.hygradle.internal.service.hytale.HytaleAccount
import java.net.URI
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Content-hashing the asset bundle is very time-consuming.")
abstract class DownloadAssets : DefaultTask() {
  @get:ServiceReference abstract val account: Property<HytaleAccountService>

  @get:ServiceReference abstract val hytale: Property<HytaleAccount>

  @get:Input abstract val version: Property<String>

  @get:Input abstract val patchline: Property<Patchline>

  @get:OutputDirectory abstract val assetBundleCacheDirectory: DirectoryProperty

  init {
    assetBundleCacheDirectory.convention(
        project.layout.projectDirectory.dir(".gradle/caches/hygradle/bundles")
    )
  }

  @TaskAction
  fun downloadAssets() {
    val patchline = patchline.get()
    val version = version.get()
    val cacheDir = assetBundleCacheDirectory.get()
    val assetBundle = cacheDir.file("$patchline-$version.zip").asFile

    // TODO: Better way to detect stale bundles? Last modified maybe??
    cacheDir.asFileTree.visit { if (file != assetBundle) file.delete() }

    // TODO: Figure out how to better avoid downloading with finer-grained caching? idk
    if (assetBundle.exists()) return

    val bundleUrl = hytale.get().service.getAssetBundle(patchline, version)

    // TODO: Use ktor client buffered streaming for this, through a worker??
    URI(bundleUrl).toURL().openStream().buffered().use { inputStream ->
      assetBundle.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
    }
  }
}
