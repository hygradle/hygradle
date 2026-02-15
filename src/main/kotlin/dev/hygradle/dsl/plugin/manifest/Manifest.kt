package dev.hygradle.dsl.plugin.manifest

import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface Manifest {
  @get:Input val name: Property<String>
  @get:Input val group: Property<String>
  @get:Input val version: Property<String>
  @get:Input @get:Optional val description: Property<String>
  @get:Input @get:Optional val website: Property<String>
  @get:Input val serverVersion: Property<String>
  @get:Input val mainClass: Property<String>
  @get:Input @get:Optional val authors: DomainObjectSet<Author>

  fun author(configure: Action<in Author>)

  @get:Input @get:Optional val dependencies: DomainObjectSet<Dependency>

  fun dependency(configure: Action<in Dependency>)
}
