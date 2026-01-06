package dev.hygradle.dsl.plugin.manifest

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Property

public abstract class PluginManifest @Inject constructor(pluginName: String, project: Project) {
  public abstract val name: Property<String>
  public abstract val group: Property<String>
  public abstract val version: Property<String>
  public abstract val description: Property<String>

  init {
    name.convention(pluginName)
    group.convention(project.rootProject.group.toString())
  }
}
