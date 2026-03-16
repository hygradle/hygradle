package dev.hygradle.internal.settings

import dev.hygradle.dsl.hytale.Patchline
import dev.hygradle.dsl.settings.HygradleSettings
import dev.hygradle.dsl.settings.Version
import dev.hygradle.dsl.settings.VersionedDependency
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class HygradleSettingsImpl : HygradleSettings {

  @get:Inject internal abstract val objects: ObjectFactory

  override val hytale: Version =
      objects.newInstance<Version>().apply {
        patchline.convention(Patchline.RELEASE)
        decompile.convention(false)
      }

  override fun hytale(configure: Action<in Version>) = configure.execute(hytale)

  override val hotswapAgent: VersionedDependency =
      objects.newInstance<VersionedDependency>().apply { version.convention("2.0.3") }

  override fun hotswapAgent(configure: Action<in VersionedDependency>) =
      configure.execute(hotswapAgent)

  override val harness: VersionedDependency =
      objects.newInstance<VersionedDependency>().apply { version.convention("0.0.1") }

  override fun harness(configure: Action<in VersionedDependency>) = configure.execute(harness)

  internal fun lock() {
    hytale.patchline.disallowChanges()
    hytale.version.disallowChanges()
    hytale.decompile.disallowChanges()
    hotswapAgent.version.disallowChanges()
    harness.version.disallowChanges()
  }
}
