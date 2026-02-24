package dev.hygradle.tasks

import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.deleteIfExists
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class ExecuteServerRun : JavaExec() {
  @get:Classpath abstract val classpathProvider: ConfigurableFileCollection

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val runDirectory: DirectoryProperty

  @get:Input abstract val modDirectories: MapProperty<String, Directory>

  init {
    mainClass.convention("com.hypixel.hytale.Main")
    runDirectory.convention(project.layout.projectDirectory.dir(".run").dir(name))
  }

  @TaskAction
  override fun exec() {
    val runDir = runDirectory.get().asFile
    val modDir = runDirectory.dir("mods").get()

    modDir.asFile.mkdir()

    for (entry in modDirectories.get()) {
      val dir = modDir.dir(entry.key).asFile.toPath()

      dir.deleteIfExists()
      dir.createSymbolicLinkPointingTo(entry.value.asFile.toPath())
    }

    workingDir = runDir
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args =
        listOf(
            "--disable-sentry",
            "--auth-mode=insecure",
            "--assets=\"C:\\Users\\remi\\AppData\\Roaming\\Hytale\\install\\release\\package\\game\\latest\\Assets.zip\"",
        )
    classpath(classpathProvider)

    super.exec()
  }
}
