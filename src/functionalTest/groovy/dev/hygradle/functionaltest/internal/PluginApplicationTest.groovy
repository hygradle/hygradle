package dev.hygradle.functionaltest.internal

import dev.hygradle.functionaltest.FunctionalSpec

class PluginApplicationTest extends FunctionalSpec {

	def "project plugin fails with clear error if settings plugin not applied"() {
		given:
		settingsFile << 'rootProject.name = "test"'
		buildFile << """\
			plugins { id("dev.hygradle") }
		""".stripIndent()

		when:
		def result = runner("help").buildAndFail()

		then:
		result.output.contains("BuildServiceRegistration with name 'hygradle-settings' not found")
	}

	def "repository extension is registered on project RepositoryHandler"() {
		given:
		settingsFile << """\
			plugins { id("dev.hygradle.settings") }
			hygradle {
				hytale { version = "1.0.0" }
			}
		""".stripIndent()
		buildFile << """\
			plugins { id("dev.hygradle") }
			repositories {
				(this as org.gradle.api.plugins.ExtensionAware).extensions
					.getByType(dev.hygradle.dsl.extension.Repository::class.java)
					.repositories()
			}
		""".stripIndent()

		when:
		def result = runner("help").build()

		then:
		result.output.contains("BUILD SUCCESSFUL")
	}
}
