package dev.hygradle.internal.extension

import dev.hygradle.dsl.extension.Repository
import dev.hygradle.dsl.hytale.Patchline
import java.io.File
import java.net.URI
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.maven

abstract class RepositoryExtension
@Inject
constructor(
    private val handler: RepositoryHandler,
    private val hytalePatchline: Provider<Patchline>,
    private val gradleUserHomeDir: File,
) : Repository {

  override fun repositories(): Unit =
      with(handler) {
        add(maven("https://maven.hygradle.dev"))
        add(
            maven {
              name = "hytale-decompiled-cache"
              url = gradleUserHomeDir.resolve("caches/hygradle/decompiled").toURI()
            }
        )

        add(
            maven {
              name = "hytale"
              url = URI.create(hytalePatchline.get().mavenRepository)
            }
        )

        mavenCentral()
      }
}
