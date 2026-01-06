package dev.hygradle.internal

import dev.hygradle.dsl.extensions.HygradleExtension
import dev.hygradle.internal.extensions.ArtifactTransformManager
import dev.hygradle.internal.extensions.AssetManager
import dev.hygradle.internal.extensions.ManagerExtension
import dev.hygradle.internal.extensions.PluginManager
import dev.hygradle.internal.extensions.RepositoryManager
import dev.hygradle.internal.extensions.RunManager
import kotlin.reflect.KClass
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.plugins.ide.idea.IdeaPlugin

public class HygradlePlugin : Plugin<Project> {
  internal val managers: MutableSet<ManagerExtension> = mutableSetOf()

  override fun apply(project: Project): Unit =
      with(project) {
        with(plugins) {
          apply(JavaPlugin::class.java)
          apply(IdeaPlugin::class.java)
        }

        extensions.create("hygradle", HygradleExtension::class.java)

        managerExtension(RepositoryManager::class)
        managerExtension(RunManager::class)
        managerExtension(PluginManager::class)
        managerExtension(ArtifactTransformManager::class)
        managerExtension(AssetManager::class)
      }

  private fun Project.managerExtension(extension: KClass<out ManagerExtension>) =
      managers.add(extensions.create("hygradle_${extension.simpleName}", extension.java))
}
