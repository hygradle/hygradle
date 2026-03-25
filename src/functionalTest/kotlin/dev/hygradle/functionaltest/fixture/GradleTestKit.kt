package dev.hygradle.functionaltest.fixture

import io.kotest.core.extensions.MountableExtension
import io.kotest.core.listeners.AfterEachListener
import io.kotest.core.listeners.BeforeEachListener
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

@DslMarker annotation class GradleTestKitDsl

class GradleTestKit :
    MountableExtension<GradleTestKitConfig, GradleProject>, BeforeEachListener, AfterEachListener {

  private val config = GradleTestKitConfig()
  private var current: GradleProjectState? = null
  private val handle = GradleProject { current ?: error("No active test context") }

  override fun mount(configure: GradleTestKitConfig.() -> Unit): GradleProject {
    config.apply(configure)
    return handle
  }

  override suspend fun beforeEach(testCase: TestCase) {
    val rootDir = withContext(Dispatchers.IO) { Files.createTempDirectory("gradle-testkit-") }
    current = GradleProjectState(rootDir, config)

    // Write default gradle.properties
    val allProperties = buildMap {
      if (config.isolatedProjects) put("org.gradle.unsafe.isolated-projects", "true")
      putAll(config.gradleProperties)
    }

    if (allProperties.isNotEmpty()) {
      rootDir
          .resolve("gradle.properties")
          .toFile()
          .writeText(allProperties.entries.joinToString("\n") { (k, v) -> "$k=$v" } + "\n")
    }
  }

  override suspend fun afterEach(testCase: TestCase, result: TestResult) {
    val state = current ?: return
    current = null

    val dirsToClean = buildList {
      add(state.rootDir)
      state.gradleUserHome?.let { add(it) }
      state.effectiveRootDir?.let { if (it != state.rootDir) add(it) }
    }

    for (dir in dirsToClean) {
      dir.toFile().deleteRecursively()
    }
  }
}

@GradleTestKitDsl
class GradleTestKitConfig {
  internal val baseArguments = mutableListOf<String>()
  internal val gradleProperties = mutableMapOf<String, String>()
  var isolatedProjects: Boolean = true
  var stacktrace: Boolean = true
  var withPluginClasspath: Boolean = true

  fun arguments(vararg args: String) {
    baseArguments.addAll(args)
  }

  fun gradleProperties(vararg entries: Pair<String, String>) {
    gradleProperties.putAll(entries)
  }
}

internal class GradleProjectState(
    val rootDir: Path,
    val config: GradleTestKitConfig,
) {
  val perTestArguments = mutableListOf<String>()
  val perTestProperties = mutableMapOf<String, String>()
  var gradleUserHome: Path? = null
  var effectiveRootDir: Path? = null
}

@GradleTestKitDsl
class GradleProject internal constructor(private val resolve: () -> GradleProjectState) :
    ProjectScope {

  operator fun invoke(block: GradleProject.() -> Unit): GradleProject = apply(block)

  override val projectDir: Path
    get() = resolve().rootDir

  // ── Settings File ───────────────────────────────────────────

  fun settingsFile(content: String) {
    projectDir.resolve("settings.gradle.kts").toFile().writeText(content.trimIndent() + "\n")
  }

  // ── Gradle Properties (per-test overrides) ─────────────────

  fun gradleProperties(vararg entries: Pair<String, String>) {
    val state = resolve()
    state.perTestProperties.putAll(entries)

    // Rewrite the file merging config defaults + per-test overrides
    val allProperties = buildMap {
      if (state.config.isolatedProjects) put("org.gradle.unsafe.isolated-projects", "true")
      putAll(state.config.gradleProperties)
      putAll(state.perTestProperties)
    }

    state.rootDir
        .resolve("gradle.properties")
        .toFile()
        .writeText(allProperties.entries.joinToString("\n") { (k, v) -> "$k=$v" } + "\n")
  }

  // ── Gradle User Home ──────────────────────────────────────

  fun gradleUserHome(): Path {
    val state = resolve()
    if (state.gradleUserHome == null) {
      state.gradleUserHome = Files.createTempDirectory("gradle-home-")
    }
    return state.gradleUserHome!!
  }

  // ── Settings Parent (testing as subproject) ───────────────

  fun settingsParent(childName: String = "child", block: ParentScope.() -> Unit) {
    val state = resolve()
    val parentDir = Files.createTempDirectory("gradle-parent-")
    val childDir = parentDir.resolve(childName)

    // Move current project contents into the child directory
    state.rootDir.toFile().copyRecursively(childDir.toFile())
    state.effectiveRootDir = parentDir

    ParentScope(parentDir).apply(block)
  }

  // ── Stub Maven Artifacts ──────────────────────────────────

  fun stubMavenArtifact(
      group: String,
      artifact: String,
      version: String,
      repoDir: String = "localRepo",
  ) {
    val groupPath = group.replace('.', '/')
    val artifactDir = projectDir.resolve("$repoDir/$groupPath/$artifact/$version")
    artifactDir.toFile().mkdirs()

    File(artifactDir.toFile(), "$artifact-$version.jar").outputStream().use { out ->
      JarOutputStream(out).close()
    }

    File(artifactDir.toFile(), "$artifact-$version.pom")
        .writeText(
            """
        <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>$group</groupId>
            <artifactId>$artifact</artifactId>
            <version>$version</version>
        </project>
        """
                .trimIndent() + "\n"
        )
  }

  // ── Build Execution ───────────────────────────────────────

  fun build(vararg arguments: String): BuildResult = runner(*arguments).build()

  fun buildAndFail(vararg arguments: String): BuildResult = runner(*arguments).buildAndFail()

  fun runner(vararg arguments: String): GradleRunner {
    val state = resolve()
    val allArgs = buildList {
      addAll(arguments)
      addAll(state.config.baseArguments)
      addAll(state.perTestArguments)
      if (state.config.stacktrace && "--stacktrace" !in this) add("--stacktrace")
      state.gradleUserHome?.let { add("--gradle-user-home=$it") }
    }

    val runnerDir = state.effectiveRootDir ?: state.rootDir

    return GradleRunner.create().let { runner ->
      runner.withProjectDir(runnerDir.toFile())
      runner.withArguments(allArgs)
      runner.forwardOutput()
      if (state.config.withPluginClasspath) runner.withPluginClasspath()
      runner
    }
  }

  // ── ProjectScope ──────────────────────────────────────────

  override fun buildFile(content: String) {
    projectDir.resolve("build.gradle.kts").toFile().writeText(content.trimIndent() + "\n")
  }

  override fun file(relativePath: String, content: String): File {
    val file = projectDir.resolve(relativePath).toFile()
    file.parentFile.mkdirs()
    file.writeText(content.trimIndent() + "\n")
    return file
  }

  override fun file(relativePath: String, content: ByteArray): File {
    val file = projectDir.resolve(relativePath).toFile()
    file.parentFile.mkdirs()
    file.writeBytes(content)
    return file
  }

  override fun directory(relativePath: String): Path {
    val dir = projectDir.resolve(relativePath)
    dir.toFile().mkdirs()
    return dir
  }

  override fun subproject(name: String, block: SubprojectScope.() -> Unit) {
    val subDir = projectDir.resolve(name)
    subDir.toFile().mkdirs()
    SubprojectScope(subDir).apply(block)
  }
}

@GradleTestKitDsl
interface ProjectScope {
  val projectDir: Path

  fun buildFile(content: String)

  fun file(relativePath: String, content: String): File

  fun file(relativePath: String, content: ByteArray): File

  fun directory(relativePath: String): Path

  fun subproject(name: String, block: SubprojectScope.() -> Unit)
}

@GradleTestKitDsl
class SubprojectScope(override val projectDir: Path) : ProjectScope {

  override fun buildFile(content: String) {
    projectDir.resolve("build.gradle.kts").toFile().writeText(content.trimIndent() + "\n")
  }

  override fun file(relativePath: String, content: String): File {
    val file = projectDir.resolve(relativePath).toFile()
    file.parentFile.mkdirs()
    file.writeText(content.trimIndent() + "\n")
    return file
  }

  override fun file(relativePath: String, content: ByteArray): File {
    val file = projectDir.resolve(relativePath).toFile()
    file.parentFile.mkdirs()
    file.writeBytes(content)
    return file
  }

  override fun directory(relativePath: String): Path {
    val dir = projectDir.resolve(relativePath)
    dir.toFile().mkdirs()
    return dir
  }

  override fun subproject(name: String, block: SubprojectScope.() -> Unit) {
    val subDir = projectDir.resolve(name)
    subDir.toFile().mkdirs()

    SubprojectScope(subDir).apply(block)
  }
}

@GradleTestKitDsl
class ParentScope(private val parentDir: Path) {

  fun settingsFile(content: String) {
    parentDir.resolve("settings.gradle.kts").toFile().writeText(content.trimIndent() + "\n")
  }

  fun buildFile(content: String) {
    parentDir.resolve("build.gradle.kts").toFile().writeText(content.trimIndent() + "\n")
  }

  fun gradleProperties(vararg entries: Pair<String, String>) {
    parentDir
        .resolve("gradle.properties")
        .toFile()
        .writeText(entries.joinToString("\n") { (k, v) -> "$k=$v" } + "\n")
  }
}
