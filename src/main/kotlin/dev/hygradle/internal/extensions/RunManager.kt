// package dev.hygradle.internal.extensions
//
// import dev.hygradle.dsl.extensions.HytaleSpec
// import dev.hygradle.dsl.plugin.Plugin
// import dev.hygradle.dsl.runs.Run
// import dev.hygradle.tasks.ExecuteServerRun
// import java.util.*
// import javax.inject.Inject
// import org.gradle.api.GradleException
// import org.gradle.api.Project
//
// public abstract class RunManager @Inject constructor(private val project: Project) :
//    ManagerExtension {
//  override fun configure(spec: HytaleSpec) {
//    for (run in spec.runs) {
//      val enabledPlugins =
//          run.plugins.get().map {
//            spec.plugins.findByName(it)
//                ?: throw GradleException(
//                    "No registered plugin named \"${it}\", requested by \"${run.name}\""
//                )
//          }
//
//      configureRun(run, enabledPlugins)
//    }
//  }
//
//  private fun configureRun(run: Run, plugins: List<Plugin>) {
//    project.tasks.register(
//        "run${
//          run.name.replaceFirstChar {
//            if (it.isLowerCase()) it.titlecase(
//                Locale.getDefault()
//            ) else it.toString()
//          }
//        }",
//        ExecuteServerRun::class.java,
//    ) {
//      it.group = "hygradle/runs"
//      it.plugins.set(plugins)
//    }
//  }
// }
