package dev.hygradle.dsl.extension

import dev.hygradle.dsl.hytale.Version
import dev.hygradle.dsl.plugin.Plugin
import dev.hygradle.dsl.run.Run
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer

interface Hygradle {
  val plugins: ExtensiblePolymorphicDomainObjectContainer<Plugin>

  fun plugins(action: Action<ExtensiblePolymorphicDomainObjectContainer<Plugin>>)

  val runs: ExtensiblePolymorphicDomainObjectContainer<Run>

  fun runs(configure: Action<ExtensiblePolymorphicDomainObjectContainer<Run>>)

  val hytale: Version

  fun hytale(configure: Action<in Version>)

  val hotswapAgent: HotswapAgent

  fun hotswapAgent(configure: Action<in HotswapAgent>)

  val harness: Harness

  fun harness(configure: Action<in Harness>)
}
