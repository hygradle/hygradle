package dev.hygradle.dsl.runs

import dev.hygradle.dsl.plugin.Plugin
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory

public abstract class Run
@Inject
constructor(
    private val name: String,
    private val registeredPlugins: Iterable<Plugin>,
    private val objects: ObjectFactory,
    project: Project,
) : Named {
  public abstract val gameDirectory: DirectoryProperty

  override fun getName(): String = name

  init {
    gameDirectory.convention(project.layout.projectDirectory.dir("run"))
  }
}
