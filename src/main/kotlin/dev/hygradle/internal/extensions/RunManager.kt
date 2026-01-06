package dev.hygradle.internal.extensions

import dev.hygradle.dsl.extensions.HytaleSpec
import dev.hygradle.dsl.runs.Run
import dev.hygradle.tasks.RunHytaleServer
import java.util.*
import javax.inject.Inject
import org.gradle.api.Project

public abstract class RunManager @Inject constructor(private val project: Project) :
    ManagerExtension {
  override fun configure(spec: HytaleSpec) {
    spec.runs.forEach { configureRun(it, spec) }
  }

  private fun configureRun(run: Run, spec: HytaleSpec) {
    val hytale = project.dependencyFactory.create("com.hypixel.hytale:Server:${spec.version.get()}")

    val hytaleConfiguration =
        project.configurations.create("${run.name}RunHytale") { it.dependencies.add(hytale) }

    val additionalRuntimeConfiguration =
        project.configurations.create("${run.name}RunAdditionalRuntimeConfiguration")

    additionalRuntimeConfiguration.extendsFrom(hytaleConfiguration)

    project.tasks.register(
        "runHytale${
          run.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
          }
        }",
        RunHytaleServer::class.java,
    ) {
      it.gameDirectory.set(run.gameDirectory)
      it.group = "hygradle"
      it.classpath(hytaleConfiguration)
    }
  }
}
