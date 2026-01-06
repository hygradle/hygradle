package dev.hygradle.internal.extensions

import dev.hygradle.dsl.extensions.HytaleSpec
import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.tasks.GeneratePluginManifest
import java.util.*
import javax.inject.Inject
import org.gradle.api.Project

public abstract class PluginManager @Inject constructor(private val project: Project) :
    ManagerExtension {
  override fun configure(spec: HytaleSpec) {
    spec.plugins.forEach(::configureManifest)
  }

  private fun configureManifest(plugin: Plugin) {
    project.tasks.register(
        "generate${
      plugin.name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(
            Locale.getDefault()
        ) else it.toString()
      }
    }Manifest",
        GeneratePluginManifest::class.java,
    ) {
      it.group = "hygradle/plugins/${plugin.name}"
      it.spec.set(plugin.manifestSpec)
    }
  }
}
