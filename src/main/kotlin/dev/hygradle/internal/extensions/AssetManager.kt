package dev.hygradle.internal.extensions

import dev.hygradle.dsl.extensions.HytaleSpec
import dev.hygradle.tasks.FetchGameBundle
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

public abstract class AssetManager @Inject constructor(private val project: Project) :
    ManagerExtension {
  override fun configure(spec: HytaleSpec) {
    val fetchBundle =
        project.tasks.register("fetchGameBundle", FetchGameBundle::class.java) {
          it.group = "hygradle/internal"
          it.hytaleVersion.set(spec.version)
          it.hytalePatchline.set(spec.patchline)
        }

    project.tasks.register("extractAssets", Copy::class.java) {
      it.group = "hygradle/internal"
      it.from(
          fetchBundle
              .map { t -> t.gameBundle }
              .map { f -> project.zipTree(f) }
              .map { f -> f.matching { m -> m.include("Assets.zip") }.singleFile }
      )

      it.rename {
        "$${spec.patchline.get().toString().lowercase()}-${spec.version.get()}-assets.zip"
      }

      it.into(project.layout.buildDirectory.dir("assets-extracted"))
    }
  }
}
