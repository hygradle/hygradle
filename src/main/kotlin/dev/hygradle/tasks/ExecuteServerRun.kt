package dev.hygradle.tasks

import java.nio.file.Path
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction

public abstract class ExecuteServerRun : JavaExec() {
  @get:Classpath public abstract val classpathProvider: ConfigurableFileCollection
  @get:Input public abstract val gameDirectory: Property<Path>

  //  @get:Input public abstract val plugins: ListProperty<Plugin>

  init {
    mainClass.convention("com.hypixel.hytale.Main")
    gameDirectory.convention(project.layout.projectDirectory.dir("run").dir(name).asFile.toPath())
  }

  @TaskAction
  override fun exec() {
    gameDirectory.get().toFile().mkdirs()

    setWorkingDir(gameDirectory)
    setArgs(listOf("--disable-sentry", "--auth-mode=insecure"))
    classpath(classpathProvider)
    super.exec()
  }

  private fun prepareResourceDirectories() {}
}
