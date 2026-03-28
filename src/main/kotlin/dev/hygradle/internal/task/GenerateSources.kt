package dev.hygradle.internal.task

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations

@UntrackedTask(because = "Decompilation is expensive, even a subset of the Hytale classes.")
abstract class GenerateSources : DefaultTask() {
  @get:Inject abstract val exec: ExecOperations

  @get:Classpath abstract val serverJar: ConfigurableFileCollection

  @get:Classpath abstract val vineflower: ConfigurableFileCollection

  @get:Input abstract val version: Property<String>

  @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun generate() {
    val version = version.get()
    val outputDir = outputDirectory.get().asFile
    val gavDir = File(outputDir, "com/hypixel/hytale/Server/$version")

    // Early-exit: if all output artifacts already exist, skip decompilation.
    val targetJar = File(gavDir, "Server-$version.jar")
    val sourcesJar = File(gavDir, "Server-$version-sources.jar")
    val pom = File(gavDir, "Server-$version.pom")
    val moduleMetadata = File(gavDir, "Server-$version.module")
    if (targetJar.exists() && sourcesJar.exists() && pom.exists() && moduleMetadata.exists()) return

    val workDir = File(outputDir, ".work")

    // Set up work directory FIRST — we may need it to shelter the input JAR
    workDir.deleteRecursively()
    workDir.mkdirs()

    // Resolve the server JAR BEFORE cleaning gavDir.  When the decompiled-cache
    // Maven repo wins resolution, this file lives inside gavDir — relocate it
    // to the work directory so the upcoming deleteRecursively() doesn't destroy
    // our own input.  Use canonical paths to handle macOS /var → /private/var symlinks.
    val resolvedJar = serverJar.singleFile
    val serverJar =
        if (resolvedJar.canonicalFile.startsWith(gavDir.canonicalFile)) {
          File(workDir, resolvedJar.name).also { resolvedJar.copyTo(it) }
        } else {
          resolvedJar
        }

    gavDir.deleteRecursively()
    gavDir.mkdirs()

    // Decompile only com/hypixel/hytale classes, outputting directly to a sources JAR
    exec.javaexec {
      classpath(vineflower)
      mainClass.set("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler")
      args("-only=com/hypixel/hytale", serverJar.absolutePath, sourcesJar.absolutePath)
      isIgnoreExitValue = true
    }

    serverJar.copyTo(targetJar, overwrite = true)

    pom.writeText(
        """
      |<?xml version="1.0" encoding="UTF-8"?>
      |<!-- This module was also published with a richer model, Gradle metadata,  -->
      |<!-- which should be used instead. Do not delete the following line which  -->
      |<!-- is to indicate to Gradle or any Gradle module metadata file consumer  -->
      |<!-- that they should prefer consuming it instead. -->
      |<!-- do_not_remove: published-with-gradle-metadata -->
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

    moduleMetadata.writeText(
        """
      |{
      |  "formatVersion": "1.1",
      |  "component": {
      |    "group": "com.hypixel.hytale",
      |    "module": "Server",
      |    "version": "$version",
      |    "attributes": {
      |      "org.gradle.status": "release"
      |    }
      |  },
      |  "variants": [
      |    {
      |      "name": "apiElements",
      |      "attributes": {
      |        "org.gradle.category": "library",
      |        "org.gradle.dependency.bundling": "external",
      |        "org.gradle.libraryelements": "jar",
      |        "org.gradle.usage": "java-api"
      |      },
      |      "files": [
      |        {
      |          "name": "Server-$version.jar",
      |          "url": "Server-$version.jar"
      |        }
      |      ]
      |    },
      |    {
      |      "name": "runtimeElements",
      |      "attributes": {
      |        "org.gradle.category": "library",
      |        "org.gradle.dependency.bundling": "external",
      |        "org.gradle.libraryelements": "jar",
      |        "org.gradle.usage": "java-runtime"
      |      },
      |      "files": [
      |        {
      |          "name": "Server-$version.jar",
      |          "url": "Server-$version.jar"
      |        }
      |      ]
      |    },
      |    {
      |      "name": "sourcesElements",
      |      "attributes": {
      |        "org.gradle.category": "documentation",
      |        "org.gradle.dependency.bundling": "external",
      |        "org.gradle.docstype": "sources",
      |        "org.gradle.usage": "java-runtime"
      |      },
      |      "files": [
      |        {
      |          "name": "Server-$version-sources.jar",
      |          "url": "Server-$version-sources.jar"
      |        }
      |      ]
      |    }
      |  ]
      |}
      """
            .trimMargin()
    )

    // Clean up working directory
    workDir.deleteRecursively()
  }
}
