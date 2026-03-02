package dev.hygradle.internal.extension

import dev.hygradle.dsl.extension.Harness
import dev.hygradle.dsl.extension.HotswapAgent
import dev.hygradle.dsl.extension.Hygradle
import dev.hygradle.dsl.hytale.Version
import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.dsl.run.Run
import dev.hygradle.internal.hytale.VersionImpl
import dev.hygradle.internal.plugin.LatePluginImpl
import dev.hygradle.internal.run.RunImpl
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class HygradleExtension @Inject constructor(objects: ObjectFactory) : Hygradle {
  override val hytale: Version = objects.newInstance<VersionImpl>()

  override fun hytale(configure: Action<in Version>) = configure.execute(hytale)

  override val hotswapAgent: HotswapAgent = objects.newInstance<HotswapAgentExtension>()

  override fun hotswapAgent(configure: Action<in HotswapAgent>) = configure.execute(hotswapAgent)

  override val harness: Harness = objects.newInstance<HarnessExtension>()

  override fun harness(configure: Action<in Harness>) = configure.execute(harness)

  override fun plugins(action: Action<ExtensiblePolymorphicDomainObjectContainer<Plugin>>) =
      action.execute(this.plugins)

  override fun runs(configure: Action<ExtensiblePolymorphicDomainObjectContainer<Run>>) =
      configure.execute(this.runs)

  init {
    plugins.registerBinding(LatePlugin::class.java, LatePluginImpl::class.java)
    runs.registerBinding(Run::class.java, RunImpl::class.java)
  }
}

internal fun Project.hygradle() = extensions.getByType(HygradleExtension::class.java)!!
