package dev.hygradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class PrepareServerRun : DefaultTask() {
  @TaskAction fun prepareRun() {}
}
