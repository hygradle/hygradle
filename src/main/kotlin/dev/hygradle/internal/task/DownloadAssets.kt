package dev.hygradle.internal.task

import dev.hygradle.dsl.hytale.Patchline
import dev.hygradle.internal.service.HytaleAccountService
import java.net.URI
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class DownloadAssets : DefaultTask() {
  @get:ServiceReference abstract val account: Property<HytaleAccountService>

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
    val patchline = patchline.get().toString().lowercase()
    val version = version.get()
    val cacheDir = assetBundleCacheDirectory.get()
    val assetBundle = cacheDir.file("$patchline-$version.zip").asFile

    // TODO: Better way to detect stale bundles? Last modified maybe??
    cacheDir.asFileTree.visit { if (file != assetBundle) file.delete() }

    // TODO: Figure out how to better avoid downloading with finer-grained caching? idk
    if (assetBundle.exists()) return

    println("Downloading asset bundle for $patchline version $version...")

    val bundleUrl = account.get().getAssetBundle(patchline, version)

    URI(bundleUrl).toURL().openStream().buffered().use { inputStream ->
      assetBundle.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
    }
  }
}
