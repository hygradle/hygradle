// package dev.hygradle.internal.extensions
//
// import dev.hygradle.dsl.extensions.HytaleSpec
// import dev.hygradle.dsl.plugin.Plugin
// import dev.hygradle.tasks.GeneratePluginManifest
// import java.util.*
// import javax.inject.Inject
// import org.gradle.api.Project
// import org.gradle.api.provider.Property
//
// public abstract class PluginManager @Inject constructor(private val project: Project) :
//    ManagerExtension {
//  override fun configure(spec: HytaleSpec) {
////    for (plugin in spec.plugins) {
////      configureManifest(plugin)
////      addHytaleDependency(plugin, spec.version)
////    }
//  }
//
//  private fun addHytaleDependency(
//      plugin: Plugin,
//      version: Property<String>,
//  ) {
//    println("Adding dependency")
//    project.dependencies.add(
//        plugin.pluginSourceSet.get().name,
//        project.dependencyFactory.create("com.hypixel.hytale", "Server", version.get()),
//    )
//  }
//
//  private fun configureManifest(plugin: Plugin) {
//    val generateManifest =
//        project.tasks.register(
//            "generate${
//      plugin.name.replaceFirstChar {
//        if (it.isLowerCase()) it.titlecase(
//            Locale.getDefault()
//        ) else it.toString()
//      }
//    }Manifest",
//            GeneratePluginManifest::class.java,
//        ) {
//          it.group = "hygradle/plugins/${plugin.name}"
//          it.spec.set(plugin.manifestSpec)
//        }
//
//    plugin.pluginSourceSet.set(
//        generateManifest
//            .flatMap { it.manifest }
//            .map { it.asFile.parentFile }
//            .zip(
//                plugin.pluginSourceSet,
//              sourceSet.apply { resources.srcDir(file) }
//            }
//    )
//  }
// }
