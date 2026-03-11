package dev.hygradle.dsl.plugin.manifest

import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/** The manifest for a Hytale plugin. */
interface Manifest {
  /** The name of the plugin. */
  @get:Input val name: Property<String>

  /** The namespace of the plugin. */
  @get:Input val group: Property<String>

  /** The version of the plugin. */
  @get:Input val version: Property<String>

  /** The description of the plugin. */
  @get:Input @get:Optional val description: Property<String>

  @get:Input @get:Optional val website: Property<String>

  /** The Hytale server version range this plugin is compatible with. */
  @get:Input val serverVersion: Property<String>

  /** The fully qualified class name for the plugin entrypoint. */
  @get:Input val mainClass: Property<String>

  /** Whether this plugin includes an asset pack. */
  @get:Input val includesAssetPack: Property<Boolean>

  /** The credited authors of the plugin. */
  @get:Input @get:Optional val authors: DomainObjectSet<Author>

  /** Configure and add a new author to the plugin manifest. */
  fun author(configure: Action<in Author>)

  /** The dependencies of the plugin. */
  @get:Input @get:Optional val dependencies: DomainObjectSet<Dependency>

  /** Configure and add a new plugin dependency to this plugin. */
  fun dependency(configure: Action<in Dependency>)
}
