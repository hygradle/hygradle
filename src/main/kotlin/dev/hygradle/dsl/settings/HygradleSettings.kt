package dev.hygradle.dsl.settings

import org.gradle.api.Action

interface HygradleSettings {
  val hytale: Version

  fun hytale(configure: Action<in Version>)

  val hotswapAgent: VersionedDependency

  fun hotswapAgent(configure: Action<in VersionedDependency>)

  val harness: VersionedDependency

  fun harness(configure: Action<in VersionedDependency>)

  val vineflower: VersionedDependency

  fun vineflower(configure: Action<in VersionedDependency>)
}
