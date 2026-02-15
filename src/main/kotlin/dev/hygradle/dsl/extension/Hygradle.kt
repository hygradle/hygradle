package dev.hygradle.dsl.extension

import dev.hygradle.dsl.hytale.Version
import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.dsl.run.Run
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested

interface Hygradle {
  @get:Input val plugins: ExtensiblePolymorphicDomainObjectContainer<Plugin>

  fun plugins(action: Action<ExtensiblePolymorphicDomainObjectContainer<Plugin>>)

  @get:Input val runs: ExtensiblePolymorphicDomainObjectContainer<Run>

  fun runs(configure: Action<ExtensiblePolymorphicDomainObjectContainer<Run>>)

  @get:Nested val hytale: Version

  fun hytale(configure: Action<in Version>)
}
