// package dev.hygradle.internal.extensions
//
// import dev.hygradle.dsl.extensions.HytaleSpec
// import dev.hygradle.internal.RepositoriesPlugin
// import javax.inject.Inject
// import org.gradle.api.Project
//
// public abstract class RepositoryManager @Inject constructor(private val project: Project) :
//    ManagerExtension {
//  override fun configure(spec: HytaleSpec) {
//    if (!project.gradle.plugins.hasPlugin(RepositoriesPlugin::class.java))
//        project.plugins.apply(RepositoriesPlugin::class.java)
//  }
// }
