package dev.hygradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class PrepareRunDirectory : DefaultTask() {
  @get:Input abstract val runName: Property<String>

  @get:OutputDirectory abstract val runDirectory: DirectoryProperty

  init {
    runDirectory.convention(
        runName.map { project.layout.projectDirectory.dir(".hygradle/run/$it") }
    )
  }

  @TaskAction
  fun prepare() {
    runDirectory.get().asFile.mkdirs()
  }
}
