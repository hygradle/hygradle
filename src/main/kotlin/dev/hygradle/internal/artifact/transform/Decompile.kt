package dev.hygradle.internal.artifact.transform

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath

@CacheableTransform
abstract class Decompile : TransformAction<TransformParameters.None> {
  @get:Classpath @get:InputArtifact abstract val inputArtifact: Provider<FileSystemLocation>

  override fun transform(outputs: TransformOutputs) {
    val inputFile = inputArtifact.get().asFile
    println("Decompiling ${inputFile.name}...")
    val sources = outputs.file("${inputFile.nameWithoutExtension}-sources.jar")
    outputs.file(inputFile)
    inputFile.copyTo(sources)
    //
    //    val decompiler =
    // Decompiler.Builder().inputs(inputFile).output(SingleFileSaver(sources)).build()
    //
    //    decompiler.decompile()
  }
}
