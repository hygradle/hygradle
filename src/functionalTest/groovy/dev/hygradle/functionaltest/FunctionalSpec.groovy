package dev.hygradle.functionaltest

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

abstract class FunctionalSpec extends Specification {

	@TempDir
	Path projectDir

	File getSettingsFile() {
		projectDir.resolve("settings.gradle.kts").toFile()
	}

	File getBuildFile() {
		projectDir.resolve("build.gradle.kts").toFile()
	}

	File getGradleProperties() {
		projectDir.resolve("gradle.properties").toFile()
	}

	def setup() {
		gradleProperties << "org.gradle.unsafe.isolated-projects=true\n"
	}

	protected List<String> baseRunnerArgs() {
		["--stacktrace"]
	}

	GradleRunner runner(String... args) {
		GradleRunner.create()
				.withProjectDir(projectDir.toFile())
				.withPluginClasspath()
				.withArguments(args.toList() + baseRunnerArgs())
				.forwardOutput()
	}
}
