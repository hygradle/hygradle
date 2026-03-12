package dev.hygradle.dsl.extension

import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.dsl.run.Run
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer

interface Hygradle {
  val plugins: ExtensiblePolymorphicDomainObjectContainer<Plugin>

  fun plugins(action: Action<ExtensiblePolymorphicDomainObjectContainer<Plugin>>)

  val runs: ExtensiblePolymorphicDomainObjectContainer<Run>

  fun runs(configure: Action<ExtensiblePolymorphicDomainObjectContainer<Run>>)
}
