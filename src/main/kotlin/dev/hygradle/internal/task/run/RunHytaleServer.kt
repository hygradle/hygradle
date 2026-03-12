package dev.hygradle.internal.task.run

import dev.hygradle.internal.service.hytale.HytaleAccount
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
  @get:ServiceReference abstract val hytale: Property<HytaleAccount>

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
    val profiles = hytale.get().service.getAvailableProfiles()
    val firstProfile = profiles.first()

    logger.lifecycle("Creating session for user '${firstProfile.username}'...")

    // TODO: Allow the user to pass their UUID as a config option
    val sessionTokens = hytale.get().service.createGameSession(profiles.first().uuid)

    val runDir = runDirectory.get().asFile

    runDir.mkdirs()

    workingDir(runDir)

    environment("HYTALE_SERVER_SESSION_TOKEN", sessionTokens.sessionToken)
    environment("HYTALE_SERVER_IDENTITY_TOKEN", sessionTokens.identityToken)

    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-XX:+AllowEnhancedClassRedefinition",
        "-XX:HotswapAgent=external",
        "-javaagent:${hotswapAgent.singleFile}",
    )

    standardInput = System.`in`

    args =
        listOf(
            "--assets",
            assets.singleFile.toString(),
            "--disable-sentry",
            "--accept-early-plugins",
            "--early-plugins",
            harness.singleFile.parentFile.toString(),
        )

    classpath(classpathProvider, harness)

    super.exec()

    /*
    TODO: Do we need to revoke the session token here? The server revokes sessions on shutdown, but that assumes it shuts down gracefully. Maybe this needs to be in a task finalizer?
    */
  }
}
