package dev.hygradle.dsl.kotlin.accessors

import dev.hygradle.dsl.extension.Repository
import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.ExtensionAware

// TODO: Remove these once Gradle actually generate accessors for settings extensions :/
val RepositoryHandler.hygradle: Repository
  get() = (this as ExtensionAware).extensions.getByType(Repository::class.java)

fun RepositoryHandler.hygradle(configure: Action<in Repository>) = configure.execute(hygradle)
