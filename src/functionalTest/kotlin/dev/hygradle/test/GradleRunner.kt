package dev.hygradle.test

import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner

fun gradleRunner(
    projectDir: Path,
    arguments: Iterable<String>,
    warningsAsErrors: Boolean = true,
    block: GradleRunner.() -> Unit = {},
): GradleRunner =
    GradleRunner.create()
        //        .withGradleVersion(testGradleVersion)
        .forwardOutput()
        .withPluginClasspath()
        //        .withTestKitDir(testKitDir.toFile())
        .withArguments(
            buildList {
              addAll(arguments)
              if (warningsAsErrors) {
                add("--warning-mode=fail")
              }
            }
        )
        .withProjectDir(projectDir.toFile())
        .apply(block)
