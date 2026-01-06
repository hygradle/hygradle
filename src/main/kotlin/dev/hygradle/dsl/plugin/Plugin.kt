package dev.hygradle.dsl.plugin

import dev.hygradle.dsl.plugin.manifest.PluginManifest
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

public abstract class Plugin
@Inject
constructor(private val name: String, objects: ObjectFactory, project: Project) : Named {
  internal val pluginSourceSet: Property<SourceSet> = objects.property(SourceSet::class.java)

  internal val manifestSpec: PluginManifest = objects.newInstance(PluginManifest::class.java, name)

  init {
    pluginSourceSet
        .convention(
            project.extensions
                .findByType(SourceSetContainer::class.java)!!
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        )
        .finalizeValueOnRead()
  }

  override fun getName(): String = name

  public fun sourceSet(sourceSet: SourceSet): Unit = pluginSourceSet.set(sourceSet)

  public fun manifest(configure: Action<PluginManifest>): Unit = configure.execute(manifestSpec)
}
