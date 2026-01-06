package dev.hygradle.tasks

import de.undercouch.gradle.tasks.download.DownloadAction
import dev.hygradle.internal.HytalePatchline
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
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
  private val downloadAction: DownloadAction = DownloadAction(project, this)

  @get:Input public abstract val hytaleVersion: Property<String>
  @get:Input public abstract val hytalePatchline: Property<HytalePatchline>

  @get:OutputFile public abstract val gameBundle: RegularFileProperty

  init {
    gameBundle.convention(project.layout.projectDirectory.dir("tmp").file("assets.zip"))
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
                "https://account-data.hytale.com/game-assets/builds/${hytalePatchline.get().toString().lowercase()}/${hytaleVersion.get()}.zip"
            )
            .body<BundleResponse>()

    downloadAction
        .apply {
          src(bundleResponse.url)
          dest(gameBundle.get().asFile)
        }
        .execute()
  }
}

@Serializable public data class BundleResponse(val url: String)
