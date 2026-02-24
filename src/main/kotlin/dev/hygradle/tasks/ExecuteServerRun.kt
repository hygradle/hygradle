package dev.hygradle.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Classpath
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

  init {
    mainClass.convention("com.hypixel.hytale.Main")
  }

  @TaskAction
  override fun exec() {
    val runDir = runDirectory.get().asFile

    runDir.mkdirs()

    workingDir(runDir)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args = listOf("--disable-sentry", "--auth-mode=insecure")
    classpath(classpathProvider)
    super.exec()
  }
}
