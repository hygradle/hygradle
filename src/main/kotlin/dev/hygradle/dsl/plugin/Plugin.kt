package dev.hygradle.dsl.plugin

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles

interface Plugin : Named {
  @get:InputFiles val runtimeClasspath: ConfigurableFileCollection
  @get:InputFiles val compileClasspath: ConfigurableFileCollection
}
