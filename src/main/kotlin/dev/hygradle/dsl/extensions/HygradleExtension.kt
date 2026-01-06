package dev.hygradle.dsl.extensions

import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.dsl.runs.Run
import dev.hygradle.internal.HygradlePlugin
import dev.hygradle.internal.HytalePatchline
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

public abstract class HygradleExtension
@Inject
constructor(private val objects: ObjectFactory, private val project: Project) {
  public fun hytale(configure: Action<HytaleSpec>) {
    val spec = objects.newInstance(HytaleSpec::class.java)
    configure.execute(spec)

    val rootPlugin: HygradlePlugin = project.plugins.findPlugin("dev.hygradle") as HygradlePlugin

    rootPlugin.managers.forEach { it.configure(spec) }
  }
}

public abstract class HytaleSpec {
  public abstract val version: Property<String>
  public abstract val patchline: Property<HytalePatchline>
  internal abstract val plugins: NamedDomainObjectContainer<Plugin>
  internal abstract val runs: NamedDomainObjectContainer<Run>

  public fun plugin(name: String, configure: Action<Plugin>): Plugin =
      plugins.create(name, configure)

  public fun run(name: String, configure: Action<Run>): Run = runs.create(name, configure)
}
