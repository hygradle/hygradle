package dev.hygradle.test.internal

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

import org.gradle.testkit.runner.GradleRunner

class RepositoryPluginTest extends Specification {

    @TempDir
    Path projectDir

    def "settings plugin configures repositories with repositoriesMode #mode"() {
        given:
        settingsFile << """
            plugins { id 'dev.hygradle.repositories' }
            dependencyResolutionManagement {
                repositoriesMode = RepositoriesMode.${mode}
                repositories { mavenCentral() }
            }
        """
        buildFile << ""

        when:
        def result = runner("help").build()

        then:
        result.output.contains("BUILD SUCCESSFUL")

        where:
        mode << ["PREFER_PROJECT", "PREFER_SETTINGS", "FAIL_ON_PROJECT_REPOS"]
    }

    def "project plugin adds repositories when settings plugin is not applied"() {
        given:
        settingsFile << "rootProject.name = 'test'"
        buildFile << """
            plugins { id 'dev.hygradle' }
            tasks.register("printRepos") {
                doLast { repositories.each { println "REPO: \${it.name}" } }
            }
        """

        when:
        def result = runner("printRepos").build()

        then:
        result.output.contains("REPO: hytale-release")
        result.output.contains("REPO: hytale-prerelease")
    }

    def "project plugin does not add project-level repositories when settings plugin was applied"() {
        given:
        settingsFile << """
            plugins { id 'dev.hygradle.repositories' }
            dependencyResolutionManagement {
                repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
                repositories { mavenCentral() }
            }
        """
        buildFile << """
            plugins { id 'dev.hygradle' }
        """

        when:
        def result = runner("help").build()

        then:
        result.output.contains("BUILD SUCCESSFUL")
    }

    private GradleRunner runner(String... args) {
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(args.toList() + ["--stacktrace"])
            .forwardOutput()
    }

    private File getSettingsFile() { projectDir.resolve("settings.gradle").toFile() }

    private File getBuildFile() { projectDir.resolve("build.gradle").toFile() }
}