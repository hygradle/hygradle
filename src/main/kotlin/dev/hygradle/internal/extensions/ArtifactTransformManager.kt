package dev.hygradle.internal.extensions

import dev.hygradle.dsl.extensions.HytaleSpec
import dev.hygradle.internal.artifact.transform.Decompile
import javax.inject.Inject
import org.gradle.api.Project

public abstract class ArtifactTransformManager @Inject constructor(project: Project) :
    ManagerExtension {
  init {
    Decompile.register(project)
  }

  override fun configure(spec: HytaleSpec) {}
}
