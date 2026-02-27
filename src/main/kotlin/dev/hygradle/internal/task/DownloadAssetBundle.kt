package dev.hygradle.internal.task

import de.undercouch.gradle.tasks.download.DownloadAction
import dev.hygradle.dsl.hytale.Patchline
import dev.hygradle.internal.service.auth.AuthService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class DownloadAssetBundle : DefaultTask() {
  @get:Internal protected val downloadAction = DownloadAction(project, this)

  @get:ServiceReference abstract val auth: Property<AuthService>

  @get:Input abstract val version: Property<String>

  @get:Input abstract val patchline: Property<Patchline>

  @get:OutputFile abstract val assetBundle: RegularFileProperty

  init {
    assetBundle.convention(
        patchline
            .zip(
                version,
            ) { patchline, version ->
              "${patchline.toString().lowercase()}-${version}.zip"
            }
            .map {
              project.layout.projectDirectory
                  .dir(".gradle")
                  .dir("caches")
                  .dir("hygradle")
                  .dir("bundles")
                  .file(it)
            }
    )
  }

  @TaskAction
  fun downloadAssets() {
    val client = HttpClient(CIO) { install(ContentNegotiation) { json() } }

    runBlocking {
      val authToken = auth.get().getAuthTokenSuspend()

      val bundle: AssetBundle =
          client
              .get(
                  "https://account-data.hytale.com/game-assets/builds/${
                    patchline.get().toString().lowercase()
                  }/${version.get()}.zip"
              ) {
                bearerAuth(authToken.token)
              }
              .body()

      println(
          "Downloading assets for ${patchline.get().toString().lowercase()} version '${version.get()}'... "
      )

      downloadAction.src(bundle.url)
      downloadAction.dest(assetBundle.get())
      downloadAction.overwrite(true)
      downloadAction.quiet(true)
      downloadAction.execute().await()
    }
  }
}

@Serializable data class AssetBundle(val url: String)
