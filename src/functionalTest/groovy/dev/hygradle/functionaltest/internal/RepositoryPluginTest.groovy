package dev.hygradle.functionaltest.internal

import dev.hygradle.functionaltest.FunctionalSpec
import dev.hygradle.functionaltest.GradleDsl

class RepositoryPluginTest extends FunctionalSpec {

	def "hytale() repository extension adds patchline repositories (#dsl)"() {
		given:
		settingsFile(dsl) << [
			(GradleDsl.GROOVY): """\
                import static dev.hygradle.dsl.settings.RepositoriesKt.hytale
                import dev.hygradle.dsl.hytale.Patchline
                plugins { id 'dev.hygradle.settings' }
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.${mode})
                    repositories {
                        hytale(delegate, Patchline.RELEASE)
                        mavenCentral()
                    }
                }
            """,
			(GradleDsl.KOTLIN): """\
                import dev.hygradle.dsl.settings.hytale
                import dev.hygradle.dsl.hytale.Patchline
                plugins { id("dev.hygradle.settings") }
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.${mode})
                    repositories {
                        hytale(Patchline.RELEASE)
                        mavenCentral()
                    }
                }
            """
		][dsl]
		buildFile(dsl) << ""

		when:
		def result = runner("help").build()

		then:
		result.output.contains("BUILD SUCCESSFUL")

		where:
		[mode, dsl] << [
			[
				"PREFER_PROJECT",
				"PREFER_SETTINGS",
				"FAIL_ON_PROJECT_REPOS"
			],
			GradleDsl.values().toList()
		].combinations()
	}

	def "project plugin fails with clear error if settings plugin not applied (#dsl)"() {
		given:
		settingsFile(dsl) << [
			(GradleDsl.GROOVY): "rootProject.name = 'test'",
			(GradleDsl.KOTLIN): 'rootProject.name = "test"'
		][dsl]
		buildFile(dsl) << [
			(GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle' }
            """,
			(GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle") }
            """
		][dsl]

		when:
		def result = runner("help").buildAndFail()

		then:
		result.output.contains("'dev.hygradle.settings' plugin must be applied in settings.gradle.kts")

		where:
		dsl << GradleDsl.values()
	}
}
