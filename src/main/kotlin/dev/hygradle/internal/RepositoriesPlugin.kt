package dev.hygradle.internal

import dev.hygradle.internal.extension.hygradle
import java.net.URI
import org.gradle.api.Plugin
import org.gradle.api.Project

class RepositoriesPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.repositories.add(
        project
            .hygradle()
            .hytale
            .patchline
            .map {
              project.repositories.maven {
                name = "hytale-${it.name.lowercase()}"
                url = URI.create(it.repository)
              }
            }
            .get()
    )

    // TODO: Figure out why lazy adding causes a concurrent modification issue
    //    project.repositories.addLater(
    //        project.hygradle().hytale.patchline.map {
    //          project.repositories.maven {
    //            name = "hytale.${it.name.lowercase()}"
    //            url = URI.create(it.repository)
    //          }
    //        }
    //    )
  }
}
