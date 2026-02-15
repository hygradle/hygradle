package dev.hygradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

public abstract class PrepareServerRun : DefaultTask() {

  @TaskAction public fun prepareRun() {}
}
