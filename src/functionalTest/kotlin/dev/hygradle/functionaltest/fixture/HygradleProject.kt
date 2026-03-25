package dev.hygradle.functionaltest.fixture

import io.kotest.core.extensions.MountableExtension
import io.kotest.core.listeners.AfterEachListener
import io.kotest.core.listeners.BeforeEachListener
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult

@DslMarker annotation class HygradleProjectMarker

class HygradleProject :
    MountableExtension<HygradleProject.Configuration, HygradleProject.Project>,
    BeforeEachListener,
    AfterEachListener {

  override fun mount(configure: Configuration.() -> Unit): Project {
    TODO("Not yet implemented")
  }

  override suspend fun beforeEach(testCase: TestCase) {}

  override suspend fun afterEach(testCase: TestCase, result: TestResult) {}

  class Configuration {}

  @HygradleProjectMarker
  open class Project {
    private val subprojects: MutableMap<String, Project> = mutableMapOf()

    private val buildFileContents =
        """
        plugins {
          id("dev.hygradle")
        }
        """
            .trimIndent()

    fun subproject(name: String, configure: Project.() -> Unit) {
      this.subprojects[name] = Project().apply(configure)
    }
  }

  @HygradleProjectMarker
  class RootProject : Project() {
    private val settingsFileContents =
        """
        plugins {
          id("dev.hygradle.settings")
        }
        """
            .trimIndent()
  }
}
