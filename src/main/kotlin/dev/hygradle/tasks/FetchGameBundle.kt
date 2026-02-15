package dev.hygradle.tasks

import de.undercouch.gradle.tasks.download.DownloadAction
import dev.hygradle.dsl.hytale.Patchline
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
public abstract class FetchGameBundle : DefaultTask() {
  @get:Input public abstract val version: Property<String>
  @get:Input public abstract val patchline: Property<Patchline>
  @get:OutputFile public abstract val gameBundle: RegularFileProperty

  init {
    gameBundle.fileProvider(
        patchline
            .zip(
                version,
                { patchline, version -> "${patchline.toString().lowercase()}-${version}.zip" },
            )
            .map { fileName ->
              File(project.gradle.gradleUserHomeDir, "caches/hygradle/game/$fileName")
            }
    )
  }

  @TaskAction
  public fun run() {
    runBlocking { downloadGameBundle() }
  }

  private suspend fun downloadGameBundle() {
    val client = HttpClient(CIO) { install(ContentNegotiation) { json() } }

    val bundleResponse =
        client
            .get(
                "https://account-data.hytale.com/game-assets/builds/${patchline.get().toString().lowercase()}/${version.get()}.zip"
            )
            .body<BundleResponse>()

    DownloadAction(project, this)
        .apply {
          src(bundleResponse.url)
          dest(gameBundle.get())
        }
        .execute()
  }
}

@Serializable public data class BundleResponse(val url: String)
