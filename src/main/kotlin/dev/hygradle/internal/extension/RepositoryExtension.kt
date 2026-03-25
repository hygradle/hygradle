package dev.hygradle.internal.extension

import dev.hygradle.dsl.extension.Repository
import dev.hygradle.dsl.hytale.Patchline
import java.net.URI
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.maven

abstract class RepositoryExtension
@Inject
constructor(
    private val handler: RepositoryHandler,
    private val hytalePatchline: Provider<Patchline>,
) : Repository {

  @get:Inject abstract val project: Project

  override fun repositories(): Unit =
      with(handler) {
        add(maven("https://maven.hygradle.dev"))
        add(
            maven {
              name = "hytale-decompiled-cache"
              url = project.gradle.gradleUserHomeDir.resolve("caches/hygradle/decompiled").toURI()
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
