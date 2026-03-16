package dev.hygradle.internal.task.plugin

import java.io.File
import javax.inject.Inject
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.deleteRecursively
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class AssembleAssets : DefaultTask() {
  @get:Inject abstract val layout: ProjectLayout

  @get:Input abstract val pluginName: Property<String>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val pluginResources: ConfigurableFileCollection

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val pluginManifest: RegularFileProperty

  @get:OutputDirectory abstract val assetDirectory: DirectoryProperty

  init {
    assetDirectory.convention(
        layout.buildDirectory.dir("hygradle/plugins").zip(pluginName) { dir, name ->
          dir.dir("$name/assets")
        }
    )
  }

  @TaskAction
  fun prepare() {
    val assetDir = assetDirectory.get().asFile

    if (assetDir.exists()) {
      assetDir.listFiles().forEach { it.delete() }
      assetDir.delete()
    }

    assetDir.mkdirs()

    symlink(pluginManifest.get().asFile)
    symlink(ASSET_DIRECTORY_NAMES)
  }

  private fun symlink(fileNames: List<String>) {
    pluginResources.asFileTree.visit(rootDirectoryVisitor(fileNames) { symlink(file) })
  }

  @OptIn(ExperimentalPathApi::class)
  private fun symlink(file: File) =
      assetDirectory
          .get()
          .asFile
          .resolve(file.name)
          .toPath()
          .also { it.deleteRecursively() }
          .createSymbolicLinkPointingTo(file.toPath())

  companion object {
    val ASSET_DIRECTORY_NAMES = listOf("Common", "Server")
  }
}

internal fun rootDirectoryVisitor(
    names: Collection<String>,
    action: FileVisitDetails.() -> Unit,
): FileVisitor =
    object : FileVisitor {
      override fun visitDir(dirDetails: FileVisitDetails) {
        if (dirDetails.relativePath.segments.size == 1 && dirDetails.name in names) {
          action.invoke(dirDetails)
        }
      }

      override fun visitFile(fileDetails: FileVisitDetails) {}
    }
