package dev.hygradle.internal.artifact.transform

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.jetbrains.java.decompiler.api.Decompiler
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver

@CacheableTransform
public abstract class Decompile @Inject constructor(private val project: Project) :
    TransformAction<TransformParameters.None> {
  @get:Classpath @get:InputArtifact public abstract val inputArtifact: Provider<FileSystemLocation>

  override fun transform(outputs: TransformOutputs) {
    val inputFile = inputArtifact.get().asFile
    val sources = outputs.file("${inputFile.nameWithoutExtension}-sources.jar")
    outputs.file(inputFile)

    val decompiler = Decompiler.Builder().inputs(inputFile).output(SingleFileSaver(sources)).build()

    decompiler.decompile()
  }

  public companion object {
    public val DECOMPILED_ATTRIBUTE: Attribute<Boolean> =
        Attribute.of("decompiled", Boolean::class.javaObjectType)

    internal fun register(project: Project) {
      with(project.dependencies) {
        registerTransform(Decompile::class.java) { spec ->
          spec.from.attribute(DECOMPILED_ATTRIBUTE, false)
          spec.to.attribute(DECOMPILED_ATTRIBUTE, true)
        }
      }
    }
  }
}
