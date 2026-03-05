package dev.hygradle.internal.task.run

import dev.hygradle.internal.service.auth.OAuthManager
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class RunHytaleServer : JavaExec() {

  @get:ServiceReference abstract val OAuthManager: Property<OAuthManager>

  @get:Classpath abstract val classpathProvider: ConfigurableFileCollection

  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val runDirectory: DirectoryProperty

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val assets: ConfigurableFileCollection

  @get:Classpath abstract val hotswapAgent: ConfigurableFileCollection

  @get:Classpath abstract val harness: ConfigurableFileCollection

  init {
    mainClass.convention("com.hypixel.hytale.Main")
  }

  @TaskAction
  override fun exec() {
    val runDir = runDirectory.get().asFile

    runDir.mkdirs()

    workingDir(runDir)
    jvmArgs(
        "-XX:+AllowEnhancedClassRedefinition",
        "-XX:HotswapAgent=external",
        "-javaagent:${hotswapAgent.singleFile}",
    )
    standardInput = System.`in`
    args = listOf("--assets", assets.singleFile.toString(), "--disable-sentry")
    classpath(classpathProvider, harness)

    super.exec()
  }
}
