package dev.hygradle.internal.extension

import dev.hygradle.dsl.extension.Hygradle
import dev.hygradle.dsl.plugin.LatePlugin
import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.dsl.run.Run
import dev.hygradle.internal.plugin.LatePluginImpl
import dev.hygradle.internal.run.RunImpl
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

abstract class HygradleExtension : Hygradle, ExtensionAware {
  override fun plugins(action: Action<ExtensiblePolymorphicDomainObjectContainer<Plugin>>) =
      action.execute(this.plugins)

  override fun runs(configure: Action<ExtensiblePolymorphicDomainObjectContainer<Run>>) =
      configure.execute(this.runs)

  init {
    plugins.registerBinding(LatePlugin::class.java, LatePluginImpl::class.java)
    runs.registerBinding(Run::class.java, RunImpl::class.java)
  }
}

internal fun Project.hygradle() = extensions.getByType(Hygradle::class.java) as HygradleExtension
