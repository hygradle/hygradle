package dev.hygradle.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction

public abstract class RunHytaleServer : JavaExec() {
  @get:Classpath public abstract val classpathProvider: ConfigurableFileCollection

  @get:Internal public abstract val gameDirectory: DirectoryProperty

  @TaskAction
  override fun exec() {
    gameDirectory.get().asFile.mkdirs()
    classpath(classpathProvider)
    setWorkingDir(gameDirectory)
    setArgs(listOf("--disable-sentry", "--auth-mode", "insecure"))
    mainClass.set("com.hypixel.hytale.Main")
    super.exec()
  }
}
