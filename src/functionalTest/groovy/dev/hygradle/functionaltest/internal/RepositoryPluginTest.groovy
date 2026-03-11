package dev.hygradle.functionaltest.internal

import dev.hygradle.functionaltest.FunctionalSpec
import dev.hygradle.functionaltest.GradleDsl

class RepositoryPluginTest extends FunctionalSpec {

    def "settings plugin configures repositories with repositoriesMode #mode (#dsl)"() {
        given:
        settingsFile(dsl) << [
            (GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle.repositories' }
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.${mode})
                    repositories { mavenCentral() }
                }
            """,
            (GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle.repositories") }
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.${mode})
                    repositories { mavenCentral() }
                }
            """
        ][dsl]
        buildFile(dsl) << ""

        when:
        def result = runner("help").build()

        then:
        result.output.contains("BUILD SUCCESSFUL")

        where:
        [mode, dsl] << [["PREFER_PROJECT", "PREFER_SETTINGS", "FAIL_ON_PROJECT_REPOS"], GradleDsl.values().toList()].combinations()
    }

    def "project plugin adds repositories when settings plugin is not applied (#dsl)"() {
        given:
        settingsFile(dsl) << [
            (GradleDsl.GROOVY): "rootProject.name = 'test'",
            (GradleDsl.KOTLIN): 'rootProject.name = "test"'
        ][dsl]
        buildFile(dsl) << [
            (GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle' }
                tasks.register('printRepos') {
                    doLast { repositories.each { println "REPO: \${it.name}" } }
                }
            """,
            (GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle") }
                tasks.register("printRepos") {
                    doLast { project.repositories.forEach { println("REPO: \${it.name}") } }
                }
            """
        ][dsl]

        when:
        def result = runner("printRepos").build()

        then:
        result.output.contains("REPO: hytale-release")
        result.output.contains("REPO: hytale-prerelease")

        where:
        dsl << GradleDsl.values()
    }

    def "project plugin adds repositories when consumer defines project-level repositories alongside settings plugin (#dsl)"() {
        given:
        settingsFile(dsl) << [
            (GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle.repositories' }
                dependencyResolutionManagement {
                    repositories { mavenCentral() }
                }
            """,
            (GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle.repositories") }
                dependencyResolutionManagement {
                    repositories { mavenCentral() }
                }
            """
        ][dsl]
        buildFile(dsl) << [
            (GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle' }
                repositories { mavenCentral() }
                tasks.register('printRepos') {
                    doLast { repositories.each { println "REPO: \${it.name}" } }
                }
            """,
            (GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle") }
                repositories { mavenCentral() }
                tasks.register("printRepos") {
                    doLast { project.repositories.forEach { println("REPO: \${it.name}") } }
                }
            """
        ][dsl]

        when:
        def result = runner("printRepos").build()

        then:
        result.output.contains("REPO: MavenRepo")
        result.output.contains("REPO: hytale-release")
        result.output.contains("REPO: hytale-prerelease")

        where:
        dsl << GradleDsl.values()
    }

    def "project plugin does not add project-level repositories when settings plugin was applied (#dsl)"() {
        given:
        settingsFile(dsl) << [
            (GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle.repositories' }
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories { mavenCentral() }
                }
            """,
            (GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle.repositories") }
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories { mavenCentral() }
                }
            """
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
        def result = runner("help").build()

        then:
        result.output.contains("BUILD SUCCESSFUL")

        where:
        dsl << GradleDsl.values()
    }
}
