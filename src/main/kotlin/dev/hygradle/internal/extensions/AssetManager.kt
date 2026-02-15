// package dev.hygradle.internal.extensions
//
// import dev.hygradle.dsl.extensions.HytaleSpec
// import dev.hygradle.tasks.ExtractAssetBundle
// import dev.hygradle.tasks.FetchGameBundle
// import javax.inject.Inject
// import org.gradle.api.Project
//
// public abstract class AssetManager @Inject constructor(private val project: Project) :
//    ManagerExtension {
//  override fun configure(spec: HytaleSpec) {
//    val fetchBundle =
//        project.tasks.register("fetchGameBundle", FetchGameBundle::class.java) {
//          it.group = "hygradle/internal"
//          it.version.set(spec.version)
//          it.patchline.set(spec.patchline)
//        }
//
//    project.tasks.register("extractAssets", ExtractAssetBundle::class.java) {
//      it.group = "hygradle/internal"
//      it.version.set(spec.version)
//      it.patchline.set(spec.patchline)
//      it.gameBundle.set(fetchBundle.flatMap { t -> t.gameBundle })
//    }
//  }
// }
