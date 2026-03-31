package dev.hygradle.dsl.run

import org.gradle.api.Action
import org.gradle.api.Named

interface Run : Named {
  val dependencies: RunDependencies

  fun dependencies(configure: Action<in RunDependencies>)
}
