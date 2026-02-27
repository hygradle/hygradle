package dev.hygradle.tasks

import dev.hygradle.internal.service.auth.AuthService
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class ExecuteServerRun : JavaExec() {

  @get:ServiceReference abstract val hytaleAuth: Property<AuthService>

  @get:Classpath abstract val classpathProvider: ConfigurableFileCollection

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val runDirectory: DirectoryProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val assets: RegularFileProperty

  init {
    mainClass.convention("com.hypixel.hytale.Main")
  }

  @TaskAction
  override fun exec() {
    val runDir = runDirectory.get().asFile

    runDir.mkdirs()

    workingDir(runDir)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    standardInput = System.`in`
    args = listOf("--assets", assets.get().asFile.absolutePath, "--disable-sentry")
    classpath(classpathProvider)
    super.exec()
  }
}
