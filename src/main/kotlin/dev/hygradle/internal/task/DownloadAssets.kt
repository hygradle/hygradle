package dev.hygradle.internal.task

import dev.hygradle.dsl.hytale.Patchline
import dev.hygradle.internal.service.hytale.HytaleAccount
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import java.io.File
import java.util.zip.ZipFile
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Content-hashing the asset bundle is very time-consuming.")
abstract class DownloadAssets : DefaultTask() {
  @get:ServiceReference abstract val hytale: Property<HytaleAccount>

  @get:Input abstract val version: Property<String>

  @get:Input abstract val patchline: Property<Patchline>

  @get:OutputDirectory abstract val assetBundleCacheDirectory: DirectoryProperty

  @get:Internal abstract val bundleFile: RegularFileProperty

  init {
    bundleFile.convention(
        assetBundleCacheDirectory.zip(
            patchline.zip(version) { patchline, version -> "$patchline-$version.zip" }
        ) { dir, filename ->
          dir.file(filename)
        }
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

    if (assetBundle.exists() && assetBundle.isValidZip()) return

    val bundleUrl = hytale.get().service.getAssetBundle(patchline, version)

    runBlocking {
      HttpClient(CIO)
          .prepareGet(bundleUrl) {
            // TODO: Maybe make this configurable?
            timeout { requestTimeoutMillis = 30.minutes.inWholeMilliseconds }
          }
          .execute { response -> response.bodyAsChannel().copyAndClose(assetBundle.writeChannel()) }
    }
  }
}

// TODO: extract this into a proper shared utility
internal fun File.isValidZip(): Boolean =
    try {
      ZipFile(this).close()
      true
    } catch (_: Exception) {
      false
    }
