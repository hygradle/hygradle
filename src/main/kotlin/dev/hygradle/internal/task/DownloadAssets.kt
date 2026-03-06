package dev.hygradle.internal.task

import de.undercouch.gradle.tasks.download.DownloadAction
import dev.hygradle.dsl.hytale.Patchline
import dev.hygradle.internal.service.HytaleAccountService
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class DownloadAssets : DefaultTask() {
  @get:Internal protected val downloadAction = DownloadAction(project, this)

  @get:ServiceReference abstract val account: Property<HytaleAccountService>

  @get:Input abstract val version: Property<String>

  @get:Input abstract val patchline: Property<Patchline>

  @get:OutputDirectory abstract val assetBundleCacheDirectory: DirectoryProperty

  init {
    assetBundleCacheDirectory.convention(
        project.layout.projectDirectory.dir(".gradle").dir("caches").dir("hygradle").dir("bundles")
    )
  }

  @TaskAction
  fun downloadAssets() {
    val cacheDir = assetBundleCacheDirectory.get()
    val assetBundle = cacheDir.file("${patchline.get()}-${version.get()}.zip").asFile

    // TODO: Figure out if there's a better way to do caching here. Plugin updates or source changes
    // will bust the task cache and result in a full asset re-download even if the bundle is present
    // and unchanged, and this is a deliberate choice to avoid that naively for now.
    if (assetBundle.exists()) {
      println("Bundle found for ${patchline.get()} version ${version.get()}, skipping download...")
      return
    }

    val bundleUrl = account.get().getAssetBundle(version.get(), patchline.get())

    runBlocking {
      downloadAction.src(bundleUrl)
      downloadAction.dest(assetBundle)
      downloadAction.overwrite(true)
      downloadAction.quiet(true)
      downloadAction.execute().await()
    }
  }
}
