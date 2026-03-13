package dev.hygradle.internal.task

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

@CacheableTask
abstract class GenerateSources : DefaultTask() {
  @get:Inject abstract val exec: ExecOperations

  @get:Inject abstract val fs: FileSystemOperations

  @get:Inject abstract val archives: ArchiveOperations

  @get:Classpath abstract val serverJar: ConfigurableFileCollection

  @get:Classpath abstract val vineflower: ConfigurableFileCollection

  @get:Input abstract val version: Property<String>

  @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun generate() {
    val version = version.get()
    val outputDir = outputDirectory.get().asFile
    val gavDir = File(outputDir, "com/hypixel/hytale/Server/$version")
    val workDir = File(outputDir, ".work")

    gavDir.deleteRecursively()
    gavDir.mkdirs()
    workDir.deleteRecursively()

    val classesDir = File(workDir, "classes")
    val decompileDir = File(workDir, "decompiled")
    classesDir.mkdirs()
    decompileDir.mkdirs()

    val serverJar = serverJar.singleFile
    val targetJar = File(gavDir, "Server-$version.jar")
    val sourcesJar = File(gavDir, "Server-$version-sources.jar")
    val pom = File(gavDir, "Server-$version.pom")

    // Extract only com/hypixel/hytale/** classes from the server JAR
    fs.copy {
      from(archives.zipTree(serverJar))
      into(classesDir)
      include("com/hypixel/hytale/**")
    }

    // Decompile the filtered directory tree
    // TODO: Move this to a worker to prevent classpath leaks? hmm
    exec.javaexec {
      classpath(vineflower)
      mainClass.set("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler")
      args(classesDir.absolutePath, decompileDir.absolutePath)
      isIgnoreExitValue = true
    }

    // Package decompiled sources into -sources.jar
    ZipOutputStream(sourcesJar.outputStream().buffered()).use { stream ->
      decompileDir
          .walkTopDown()
          .filter { it.isFile }
          .forEach { file ->
            stream.putNextEntry(ZipEntry(file.relativeTo(decompileDir).invariantSeparatorsPath))
            file.inputStream().buffered().use { it.copyTo(stream) }
            stream.closeEntry()
          }
    }

    // Copy the original server JAR (skip when resolved from our own output directory)
    if (serverJar.canonicalPath != targetJar.canonicalPath) {
      serverJar.copyTo(targetJar, overwrite = true)
    }

    pom.writeText(
        """
      |<?xml version="1.0" encoding="UTF-8"?>
      |<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd"
      |         xmlns="http://maven.apache.org/POM/4.0.0"
      |         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |  <modelVersion>4.0.0</modelVersion>
      |  <groupId>com.hypixel.hytale</groupId>
      |  <artifactId>Server</artifactId>
      |  <version>$version</version>
      |</project>
      """
            .trimMargin()
    )

    // Clean up working directory
    workDir.deleteRecursively()
  }
}
