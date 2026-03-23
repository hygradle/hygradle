package dev.hygradle.functionaltest.internal

import dev.hygradle.functionaltest.FunctionalSpec
import dev.hygradle.functionaltest.GradleDsl
import groovy.json.JsonSlurper

import java.util.jar.JarOutputStream

class CrossProjectResolutionTest extends FunctionalSpec {

	private void createStubMavenArtifact(String groupPath, String artifactId, String version) {
		def artifactDir = projectDir.resolve("localRepo/${groupPath}/${artifactId}/${version}").toFile()
		artifactDir.mkdirs()

		new File(artifactDir, "${artifactId}-${version}.jar").bytes = createStubJar()

		new File(artifactDir, "${artifactId}-${version}.pom").text = """\
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>${groupPath.replace('/', '.')}</groupId>
                <artifactId>${artifactId}</artifactId>
                <version>${version}</version>
            </project>
        """.stripIndent()
	}

	private static byte[] createStubJar() {
		def bytes = new ByteArrayOutputStream()
		def jar = new JarOutputStream(bytes)
		jar.close()
		bytes.toByteArray()
	}

	private void setupMultiProject(GradleDsl dsl, String depScope) {
		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")

		settingsFile(dsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle.settings' }
                    hygradle {
                        hytale { version = '1.0.0' }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = file('localRepo') }
                        }
                    }
                    gradle.lifecycle.beforeProject {
                        group = 'com.example'
                        version = '1.0.0'
                    }
                    include ':pluginA', ':pluginB'
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle.settings") }
                    hygradle {
                        hytale { version = "1.0.0" }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = uri("localRepo") }
                        }
                    }
                    gradle.lifecycle.beforeProject {
                        group = "com.example"
                        version = "1.0.0"
                    }
                    include(":pluginA", ":pluginB")
                """.stripIndent()
		][dsl]

		// pluginA — defines a Hygradle plugin
		def pluginADir = projectDir.resolve("pluginA").toFile()
		pluginADir.mkdirs()
		new File(pluginADir, dsl.buildFileName) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        plugins {
                            register('pluginA', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.PluginA'
                                }
                            }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        plugins {
                            register<dev.hygradle.dsl.plugin.LatePlugin>("pluginA") {
                                manifest {
                                    mainClass = "com.example.PluginA"
                                }
                            }
                        }
                    }
                """.stripIndent()
		][dsl]

		// pluginA source
		def pluginASrc = projectDir.resolve("pluginA/src/main/java/com/example").toFile()
		pluginASrc.mkdirs()
		new File(pluginASrc, "PluginA.java") << """\
            package com.example;
            public class PluginA {}
        """.stripIndent()

		// pluginB — depends on pluginA
		def pluginBDir = projectDir.resolve("pluginB").toFile()
		pluginBDir.mkdirs()
		new File(pluginBDir, dsl.buildFileName) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        plugins {
                            register('pluginB', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.PluginB'
                                }
                                dependencies {
                                    ${depScope}(project(':pluginA'))
                                }
                            }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        plugins {
                            register<dev.hygradle.dsl.plugin.LatePlugin>("pluginB") {
                                manifest {
                                    mainClass = "com.example.PluginB"
                                }
                                dependencies {
                                    ${depScope}(project(":pluginA"))
                                }
                            }
                        }
                    }
                """.stripIndent()
		][dsl]

		// pluginB source
		def pluginBSrc = projectDir.resolve("pluginB/src/main/java/com/example").toFile()
		pluginBSrc.mkdirs()
		new File(pluginBSrc, "PluginB.java") << """\
            package com.example;
            public class PluginB {}
        """.stripIndent()
	}

	def "cross-project runtimeOnly resolves to classes directories, not JARs (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "runtimeOnly")

		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, (dsl as GradleDsl).buildFileName) << [
			(GradleDsl.GROOVY): """
                    tasks.register('printRuntimeClasspath') {
                        inputs.files(configurations.named('pluginBPluginRuntimeClasspath'))
                        doLast {
                            inputs.files.each { println "RESOLVED: \${it}" }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """
                    tasks.register("printRuntimeClasspath") {
                        inputs.files(configurations.named("pluginBPluginRuntimeClasspath"))
                        doLast {
                            inputs.files.forEach { println("RESOLVED: \$it") }
                        }
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(":pluginB:printRuntimeClasspath").build()

		then: 'resolved files include classes directory from pluginA (not a JAR)'
		result.output.contains("RESOLVED:")
		result.output.readLines().any { line ->
			line.contains("RESOLVED:") && line.contains("pluginA") && line.contains("classes")
		}
		!result.output.readLines().any { line ->
			line.contains("RESOLVED:") && line.contains("pluginA") && line.endsWith(".jar")
		}

		where:
		dsl << GradleDsl.values()
	}

	def "cross-project runtimeOnly resolves asset directory from producer (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "runtimeOnly")

		// Add resources to pluginA so AssembleAssets has something to publish
		def pluginAResources = projectDir.resolve("pluginA/src/main/resources/Server").toFile()
		pluginAResources.mkdirs()
		new File(pluginAResources, "test.txt") << "hello"

		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, (dsl as GradleDsl).buildFileName) << [
			(GradleDsl.GROOVY): """
                    tasks.register('printRuntimeClasspath') {
                        inputs.files(configurations.named('pluginBPluginRuntimeClasspath'))
                        doLast {
                            inputs.files.each { println "RESOLVED: \${it}" }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """
                    tasks.register("printRuntimeClasspath") {
                        inputs.files(configurations.named("pluginBPluginRuntimeClasspath"))
                        doLast {
                            inputs.files.forEach { println("RESOLVED: \$it") }
                        }
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(":pluginB:printRuntimeClasspath").build()

		then: 'resolved files include asset directory from pluginA'
		result.output.readLines().any { line ->
			line.contains("RESOLVED:") && line.contains("pluginA") && line.contains("assets")
		}

		where:
		dsl << GradleDsl.values()
	}

	def "cross-project LatePlugin dependency auto-wired into consumer manifest (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "runtimeOnly")
		// Set group/version so manifests have meaningful values
		settingsFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """
                    gradle.lifecycle.beforeProject {
                        group = 'com.example'
                        version = '1.0.0'
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """
                    gradle.lifecycle.beforeProject {
                        group = "com.example"
                        version = "1.0.0"
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(":pluginB:generatePluginBManifest").build()

		then: 'pluginB manifest contains auto-discovered dependency on pluginA'
		def manifestFile = projectDir.resolve("pluginB/build/hygradle/plugins/pluginB/manifest/manifest.json").toFile()
		manifestFile.exists()
		def manifest = new JsonSlurper().parse(manifestFile)
		manifest.Dependencies != null
		manifest.Dependencies['com.example:pluginA'] == '^1.0.0'

		where:
		dsl << GradleDsl.values()
	}

	def "user-declared optional manifest dependency takes precedence over auto-discovery (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "runtimeOnly")
		settingsFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """
                    gradle.lifecycle.beforeProject {
                        group = 'com.example'
                        version = '1.0.0'
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """
                    gradle.lifecycle.beforeProject {
                        group = "com.example"
                        version = "1.0.0"
                    }
                """.stripIndent()
		][dsl]

		// Override pluginB build file to include an explicit optional manifest dependency
		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, (dsl as GradleDsl).buildFileName).text = [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        plugins {
                            register('pluginB', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.PluginB'
                                    dependency {
                                        name = 'pluginA'
                                        group = 'com.example'
                                        version = '1.0.0'
                                        optional = true
                                    }
                                }
                                dependencies {
                                    runtimeOnly(project(':pluginA'))
                                }
                            }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        plugins {
                            register<dev.hygradle.dsl.plugin.LatePlugin>("pluginB") {
                                manifest {
                                    mainClass = "com.example.PluginB"
                                    dependency {
                                        name = "pluginA"
                                        group = "com.example"
                                        version = "1.0.0"
                                        optional = true
                                    }
                                }
                                dependencies {
                                    runtimeOnly(project(":pluginA"))
                                }
                            }
                        }
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(":pluginB:generatePluginBManifest").build()

		then: 'pluginA appears in OptionalDependencies, NOT in Dependencies'
		def manifestFile = projectDir.resolve("pluginB/build/hygradle/plugins/pluginB/manifest/manifest.json").toFile()
		manifestFile.exists()
		def manifest = new JsonSlurper().parse(manifestFile)
		manifest.OptionalDependencies != null
		manifest.OptionalDependencies['com.example:pluginA'] == '1.0.0'
		manifest.Dependencies == null || !manifest.Dependencies.containsKey('com.example:pluginA')

		where:
		dsl << GradleDsl.values()
	}

	def "cross-project plugin dependency resolves to classes directories for both compile and runtime (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "plugin")

		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, (dsl as GradleDsl).buildFileName) << [
			(GradleDsl.GROOVY): """
                    tasks.register('printCompileClasspath') {
                        inputs.files(configurations.named('pluginBPluginCompileClasspath'))
                        doLast {
                            inputs.files.each { println "COMPILE: \${it}" }
                        }
                    }
                    tasks.register('printRuntimeClasspath') {
                        inputs.files(configurations.named('pluginBPluginRuntimeClasspath'))
                        doLast {
                            inputs.files.each { println "RUNTIME: \${it}" }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """
                    tasks.register("printCompileClasspath") {
                        inputs.files(configurations.named("pluginBPluginCompileClasspath"))
                        doLast {
                            inputs.files.forEach { println("COMPILE: \$it") }
                        }
                    }
                    tasks.register("printRuntimeClasspath") {
                        inputs.files(configurations.named("pluginBPluginRuntimeClasspath"))
                        doLast {
                            inputs.files.forEach { println("RUNTIME: \$it") }
                        }
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(":pluginB:printCompileClasspath", ":pluginB:printRuntimeClasspath").build()

		then: 'compile classpath includes pluginA classes directory'
		result.output.readLines().any { line ->
			line.contains("COMPILE:") && line.contains("pluginA") && line.contains("classes")
		}

		and: 'runtime classpath includes pluginA classes directory'
		result.output.readLines().any { line ->
			line.contains("RUNTIME:") && line.contains("pluginA") && line.contains("classes")
		}

		where:
		dsl << GradleDsl.values()
	}

	def "cross-project plugin dependency auto-wires into consumer manifest (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "plugin")
		settingsFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """
                    gradle.lifecycle.beforeProject {
                        group = 'com.example'
                        version = '1.0.0'
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """
                    gradle.lifecycle.beforeProject {
                        group = "com.example"
                        version = "1.0.0"
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(":pluginB:generatePluginBManifest").build()

		then: 'pluginB manifest contains auto-discovered dependency on pluginA'
		def manifestFile = projectDir.resolve("pluginB/build/hygradle/plugins/pluginB/manifest/manifest.json").toFile()
		manifestFile.exists()
		def manifest = new JsonSlurper().parse(manifestFile)
		manifest.Dependencies != null
		manifest.Dependencies['com.example:pluginA'] == '^1.0.0'

		where:
		dsl << GradleDsl.values()
	}

	def "plugin dependency is not on source set runtimeClasspath (#dsl)"() {
		given:
		createStubMavenArtifact("com/example", "other", "1.0.0")
		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")

		settingsFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle.settings' }
                    hygradle {
                        hytale { version = '1.0.0' }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = file('localRepo') }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle.settings") }
                    hygradle {
                        hytale { version = "1.0.0" }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = uri("localRepo") }
                        }
                    }
                """.stripIndent()
		][dsl]

		buildFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        plugins {
                            register('myPlugin', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.MyPlugin'
                                }
                                dependencies {
                                    plugin('com.example:other:1.0.0')
                                }
                            }
                        }
                    }
                    tasks.register('printPluginCompileClasspath') {
                        inputs.files(configurations.named('myPluginPluginCompileClasspath'))
                        doLast {
                            inputs.files.each { println "PLUGIN_COMPILE: \${it}" }
                        }
                    }
                    tasks.register('printPluginRuntimeClasspath') {
                        inputs.files(configurations.named('myPluginPluginRuntimeClasspath'))
                        doLast {
                            inputs.files.each { println "PLUGIN_RUNTIME: \${it}" }
                        }
                    }
                    tasks.register('printSourceSetRuntimeClasspath') {
                        inputs.files(configurations.named('runtimeClasspath'))
                        doLast {
                            inputs.files.each { println "SS_RUNTIME: \${it}" }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        plugins {
                            register<dev.hygradle.dsl.plugin.LatePlugin>("myPlugin") {
                                manifest {
                                    mainClass = "com.example.MyPlugin"
                                }
                                dependencies {
                                    plugin("com.example:other:1.0.0")
                                }
                            }
                        }
                    }
                    tasks.register("printPluginCompileClasspath") {
                        inputs.files(configurations.named("myPluginPluginCompileClasspath"))
                        doLast {
                            inputs.files.forEach { println("PLUGIN_COMPILE: \$it") }
                        }
                    }
                    tasks.register("printPluginRuntimeClasspath") {
                        inputs.files(configurations.named("myPluginPluginRuntimeClasspath"))
                        doLast {
                            inputs.files.forEach { println("PLUGIN_RUNTIME: \$it") }
                        }
                    }
                    tasks.register("printSourceSetRuntimeClasspath") {
                        inputs.files(configurations.named("runtimeClasspath"))
                        doLast {
                            inputs.files.forEach { println("SS_RUNTIME: \$it") }
                        }
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(
				"printPluginCompileClasspath",
				"printPluginRuntimeClasspath",
				"printSourceSetRuntimeClasspath"
				).build()

		then: 'plugin compile classpath includes the dependency'
		result.output.readLines().any { line ->
			line.contains("PLUGIN_COMPILE:") && line.contains("other-1.0.0.jar")
		}

		and: 'plugin runtime classpath includes the dependency'
		result.output.readLines().any { line ->
			line.contains("PLUGIN_RUNTIME:") && line.contains("other-1.0.0.jar")
		}

		and: 'source set runtimeClasspath does NOT include the dependency'
		!result.output.readLines().any { line ->
			line.contains("SS_RUNTIME:") && line.contains("other-1.0.0.jar")
		}

		where:
		dsl << GradleDsl.values()
	}

	def "global tasks exist on root project, not on subprojects (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "runtimeOnly")

		when: 'list all tasks including root'
		def result = runner("tasks", "--all").build()

		then: 'global tasks exist on root'
		result.output.contains("downloadAssets")
		result.output.contains("extractAssets")

		and: 'global tasks are NOT duplicated on subprojects'
		!result.output.contains(":pluginA:downloadAssets")
		!result.output.contains(":pluginA:extractAssets")
		!result.output.contains(":pluginB:downloadAssets")
		!result.output.contains(":pluginB:extractAssets")

		where:
		dsl << GradleDsl.values()
	}

	def "cross-project optionalPlugin resolves to compile classpath but not runtime (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "optionalPlugin")

		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, (dsl as GradleDsl).buildFileName) << [
			(GradleDsl.GROOVY): """
                    tasks.register('printCompileClasspath') {
                        inputs.files(configurations.named('pluginBPluginCompileClasspath'))
                        doLast {
                            inputs.files.each { println "COMPILE: \${it}" }
                        }
                    }
                    tasks.register('printRuntimeClasspath') {
                        inputs.files(configurations.named('pluginBPluginRuntimeClasspath'))
                        doLast {
                            inputs.files.each { println "RUNTIME: \${it}" }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """
                    tasks.register("printCompileClasspath") {
                        inputs.files(configurations.named("pluginBPluginCompileClasspath"))
                        doLast {
                            inputs.files.forEach { println("COMPILE: \$it") }
                        }
                    }
                    tasks.register("printRuntimeClasspath") {
                        inputs.files(configurations.named("pluginBPluginRuntimeClasspath"))
                        doLast {
                            inputs.files.forEach { println("RUNTIME: \$it") }
                        }
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(":pluginB:printCompileClasspath", ":pluginB:printRuntimeClasspath").build()

		then: 'compile classpath includes pluginA classes directory'
		result.output.readLines().any { line ->
			line.contains("COMPILE:") && line.contains("pluginA") && line.contains("classes")
		}

		and: 'runtime classpath does NOT include pluginA classes directory'
		!result.output.readLines().any { line ->
			line.contains("RUNTIME:") && line.contains("pluginA") && line.contains("classes")
		}

		where:
		dsl << GradleDsl.values()
	}

	def "cross-project optionalPlugin auto-wires as OptionalDependency in manifest (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "optionalPlugin")
		settingsFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """
                    gradle.lifecycle.beforeProject {
                        group = 'com.example'
                        version = '1.0.0'
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """
                    gradle.lifecycle.beforeProject {
                        group = "com.example"
                        version = "1.0.0"
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(":pluginB:generatePluginBManifest").build()

		then: 'pluginB manifest contains auto-discovered optional dependency on pluginA'
		def manifestFile = projectDir.resolve("pluginB/build/hygradle/plugins/pluginB/manifest/manifest.json").toFile()
		manifestFile.exists()
		def manifest = new JsonSlurper().parse(manifestFile)
		manifest.OptionalDependencies != null
		manifest.OptionalDependencies['com.example:pluginA'] == '^1.0.0'
		manifest.Dependencies == null || !manifest.Dependencies.containsKey('com.example:pluginA')

		where:
		dsl << GradleDsl.values()
	}

	def "user-declared required manifest dependency takes precedence over optionalPlugin auto-discovery (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "optionalPlugin")
		settingsFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """
                    gradle.lifecycle.beforeProject {
                        group = 'com.example'
                        version = '1.0.0'
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """
                    gradle.lifecycle.beforeProject {
                        group = "com.example"
                        version = "1.0.0"
                    }
                """.stripIndent()
		][dsl]

		// Override pluginB build file to add an explicit required manifest dependency for pluginA
		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, (dsl as GradleDsl).buildFileName).text = [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        plugins {
                            register('pluginB', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.PluginB'
                                    dependency {
                                        name = 'pluginA'
                                        group = 'com.example'
                                        version = '1.0.0'
                                    }
                                }
                                dependencies {
                                    optionalPlugin(project(':pluginA'))
                                }
                            }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        plugins {
                            register<dev.hygradle.dsl.plugin.LatePlugin>("pluginB") {
                                manifest {
                                    mainClass = "com.example.PluginB"
                                    dependency {
                                        name = "pluginA"
                                        group = "com.example"
                                        version = "1.0.0"
                                    }
                                }
                                dependencies {
                                    optionalPlugin(project(":pluginA"))
                                }
                            }
                        }
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(":pluginB:generatePluginBManifest").build()

		then: 'pluginA appears in Dependencies, NOT in OptionalDependencies'
		def manifestFile = projectDir.resolve("pluginB/build/hygradle/plugins/pluginB/manifest/manifest.json").toFile()
		manifestFile.exists()
		def manifest = new JsonSlurper().parse(manifestFile)
		manifest.Dependencies != null
		manifest.Dependencies['com.example:pluginA'] == '1.0.0'
		manifest.OptionalDependencies == null || !manifest.OptionalDependencies.containsKey('com.example:pluginA')

		where:
		dsl << GradleDsl.values()
	}

	def "externalPlugins resolves classes to run classpath (#dsl)"() {
		given:
		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")

		settingsFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle.settings' }
                    hygradle {
                        hytale { version = '1.0.0' }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = file('localRepo') }
                        }
                    }
                    gradle.lifecycle.beforeProject {
                        group = 'com.example'
                        version = '1.0.0'
                    }
                    include ':pluginA'
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle.settings") }
                    hygradle {
                        hytale { version = "1.0.0" }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = uri("localRepo") }
                        }
                    }
                    gradle.lifecycle.beforeProject {
                        group = "com.example"
                        version = "1.0.0"
                    }
                    include(":pluginA")
                """.stripIndent()
		][dsl]

		buildFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        runs {
                            register('test') {
                                externalPlugins(project(':pluginA'))
                            }
                        }
                    }
                    tasks.register('printRunClasspath') {
                        inputs.files(configurations.named('_testExternalPluginClasspath'))
                        doLast {
                            inputs.files.each { println "RESOLVED: \${it}" }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        runs {
                            register("test") {
                                externalPlugins(project(":pluginA"))
                            }
                        }
                    }
                    tasks.register("printRunClasspath") {
                        inputs.files(configurations.named("_testExternalPluginClasspath"))
                        doLast {
                            inputs.files.forEach { println("RESOLVED: \$it") }
                        }
                    }
                """.stripIndent()
		][dsl]

		def pluginADir = projectDir.resolve("pluginA").toFile()
		pluginADir.mkdirs()
		new File(pluginADir, (dsl as GradleDsl).buildFileName) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        plugins {
                            register('pluginA', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.PluginA'
                                }
                            }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        plugins {
                            register<dev.hygradle.dsl.plugin.LatePlugin>("pluginA") {
                                manifest {
                                    mainClass = "com.example.PluginA"
                                }
                            }
                        }
                    }
                """.stripIndent()
		][dsl]

		def pluginASrc = projectDir.resolve("pluginA/src/main/java/com/example").toFile()
		pluginASrc.mkdirs()
		new File(pluginASrc, "PluginA.java") << """\
            package com.example;
            public class PluginA {}
        """.stripIndent()

		when:
		def result = runner("printRunClasspath").build()

		then: 'resolved files include classes directory from pluginA'
		result.output.contains("RESOLVED:")
		result.output.readLines().any { line ->
			line.contains("RESOLVED:") && line.contains("pluginA") && line.contains("classes")
		}

		where:
		dsl << GradleDsl.values()
	}

	def "externalPlugins resolves assets to run classpath (#dsl)"() {
		given:
		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")

		settingsFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle.settings' }
                    hygradle {
                        hytale { version = '1.0.0' }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = file('localRepo') }
                        }
                    }
                    gradle.lifecycle.beforeProject {
                        group = 'com.example'
                        version = '1.0.0'
                    }
                    include ':pluginA'
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle.settings") }
                    hygradle {
                        hytale { version = "1.0.0" }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = uri("localRepo") }
                        }
                    }
                    gradle.lifecycle.beforeProject {
                        group = "com.example"
                        version = "1.0.0"
                    }
                    include(":pluginA")
                """.stripIndent()
		][dsl]

		buildFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        runs {
                            register('test') {
                                externalPlugins(project(':pluginA'))
                            }
                        }
                    }
                    tasks.register('printRunClasspath') {
                        inputs.files(configurations.named('_testExternalPluginClasspath'))
                        doLast {
                            inputs.files.each { println "RESOLVED: \${it}" }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        runs {
                            register("test") {
                                externalPlugins(project(":pluginA"))
                            }
                        }
                    }
                    tasks.register("printRunClasspath") {
                        inputs.files(configurations.named("_testExternalPluginClasspath"))
                        doLast {
                            inputs.files.forEach { println("RESOLVED: \$it") }
                        }
                    }
                """.stripIndent()
		][dsl]

		def pluginADir = projectDir.resolve("pluginA").toFile()
		pluginADir.mkdirs()
		new File(pluginADir, (dsl as GradleDsl).buildFileName) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        plugins {
                            register('pluginA', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.PluginA'
                                }
                            }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        plugins {
                            register<dev.hygradle.dsl.plugin.LatePlugin>("pluginA") {
                                manifest {
                                    mainClass = "com.example.PluginA"
                                }
                            }
                        }
                    }
                """.stripIndent()
		][dsl]

		def pluginASrc = projectDir.resolve("pluginA/src/main/java/com/example").toFile()
		pluginASrc.mkdirs()
		new File(pluginASrc, "PluginA.java") << """\
            package com.example;
            public class PluginA {}
        """.stripIndent()

		def pluginAResources = projectDir.resolve("pluginA/src/main/resources/Server").toFile()
		pluginAResources.mkdirs()
		new File(pluginAResources, "test.txt") << "hello"

		when:
		def result = runner("printRunClasspath").build()

		then: 'resolved files include asset directory from pluginA'
		result.output.readLines().any { line ->
			line.contains("RESOLVED:") && line.contains("pluginA") && line.contains("assets")
		}

		where:
		dsl << GradleDsl.values()
	}

	def "externalPlugins with multi-plugin project resolves all plugins (#dsl)"() {
		given:
		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")

		settingsFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle.settings' }
                    hygradle {
                        hytale { version = '1.0.0' }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = file('localRepo') }
                        }
                    }
                    gradle.lifecycle.beforeProject {
                        group = 'com.example'
                        version = '1.0.0'
                    }
                    include ':shared'
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle.settings") }
                    hygradle {
                        hytale { version = "1.0.0" }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = uri("localRepo") }
                        }
                    }
                    gradle.lifecycle.beforeProject {
                        group = "com.example"
                        version = "1.0.0"
                    }
                    include(":shared")
                """.stripIndent()
		][dsl]

		buildFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        runs {
                            register('test') {
                                externalPlugins(project(':shared'))
                            }
                        }
                    }
                    tasks.register('printRunClasspath') {
                        inputs.files(configurations.named('_testExternalPluginClasspath'))
                        doLast {
                            inputs.files.each { println "RESOLVED: \${it}" }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        runs {
                            register("test") {
                                externalPlugins(project(":shared"))
                            }
                        }
                    }
                    tasks.register("printRunClasspath") {
                        inputs.files(configurations.named("_testExternalPluginClasspath"))
                        doLast {
                            inputs.files.forEach { println("RESOLVED: \$it") }
                        }
                    }
                """.stripIndent()
		][dsl]

		def sharedDir = projectDir.resolve("shared").toFile()
		sharedDir.mkdirs()
		new File(sharedDir, (dsl as GradleDsl).buildFileName) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        plugins {
                            register('alpha', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.Alpha'
                                }
                                sourceSet(sourceSets.register('alphaSrc'))
                            }
                            register('beta', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.Beta'
                                }
                                sourceSet(sourceSets.register('betaSrc'))
                            }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        plugins {
                            register<dev.hygradle.dsl.plugin.LatePlugin>("alpha") {
                                manifest {
                                    mainClass = "com.example.Alpha"
                                }
                                sourceSet(sourceSets.register("alphaSrc"))
                            }
                            register<dev.hygradle.dsl.plugin.LatePlugin>("beta") {
                                manifest {
                                    mainClass = "com.example.Beta"
                                }
                                sourceSet(sourceSets.register("betaSrc"))
                            }
                        }
                    }
                """.stripIndent()
		][dsl]

		def alphaSrc = projectDir.resolve("shared/src/alphaSrc/java/com/example").toFile()
		alphaSrc.mkdirs()
		new File(alphaSrc, "Alpha.java") << """\
            package com.example;
            public class Alpha {}
        """.stripIndent()
		def betaSrc = projectDir.resolve("shared/src/betaSrc/java/com/example").toFile()
		betaSrc.mkdirs()
		new File(betaSrc, "Beta.java") << """\
            package com.example;
            public class Beta {}
        """.stripIndent()

		when:
		def result = runner("printRunClasspath").build()

		then: 'resolved files include classes from shared project (both plugins share the same source set)'
		result.output.contains("RESOLVED:")
		result.output.readLines().any { line ->
			line.contains("RESOLVED:") && line.contains("shared") && line.contains("classes")
		}

		where:
		dsl << GradleDsl.values()
	}

	def "mixed local and external plugins in same run (#dsl)"() {
		given:
		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")

		settingsFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle.settings' }
                    hygradle {
                        hytale { version = '1.0.0' }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = file('localRepo') }
                        }
                    }
                    gradle.lifecycle.beforeProject {
                        group = 'com.example'
                        version = '1.0.0'
                    }
                    include ':external'
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle.settings") }
                    hygradle {
                        hytale { version = "1.0.0" }
                    }
                    dependencyResolutionManagement {
                        repositories {
                            maven { url = uri("localRepo") }
                        }
                    }
                    gradle.lifecycle.beforeProject {
                        group = "com.example"
                        version = "1.0.0"
                    }
                    include(":external")
                """.stripIndent()
		][dsl]

		buildFile(dsl as GradleDsl) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        plugins {
                            register('local', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.Local'
                                }
                            }
                        }
                        runs {
                            register('test') {
                                externalPlugins(project(':external'))
                            }
                        }
                    }
                    tasks.register('printRunClasspath') {
                        inputs.files(configurations.named('localPluginRuntimeClasspath'))
                        inputs.files(configurations.named('_testExternalPluginClasspath'))
                        doLast {
                            inputs.files.each { println "CLASSPATH: \${it}" }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        plugins {
                            register<dev.hygradle.dsl.plugin.LatePlugin>("local") {
                                manifest {
                                    mainClass = "com.example.Local"
                                }
                            }
                        }
                        runs {
                            register("test") {
                                externalPlugins(project(":external"))
                            }
                        }
                    }
                    tasks.register("printRunClasspath") {
                        inputs.files(configurations.named("localPluginRuntimeClasspath"))
                        inputs.files(configurations.named("_testExternalPluginClasspath"))
                        doLast {
                            inputs.files.forEach { println("CLASSPATH: \$it") }
                        }
                    }
                """.stripIndent()
		][dsl]

		def localSrc = projectDir.resolve("src/main/java/com/example").toFile()
		localSrc.mkdirs()
		new File(localSrc, "Local.java") << """\
            package com.example;
            public class Local {}
        """.stripIndent()

		def externalDir = projectDir.resolve("external").toFile()
		externalDir.mkdirs()
		new File(externalDir, (dsl as GradleDsl).buildFileName) << [
			(GradleDsl.GROOVY): """\
                    plugins { id 'dev.hygradle' }
                    hygradle {
                        plugins {
                            register('extPlugin', dev.hygradle.dsl.plugin.LatePlugin) {
                                manifest {
                                    mainClass = 'com.example.ExtPlugin'
                                }
                            }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                    plugins { id("dev.hygradle") }
                    hygradle {
                        plugins {
                            register<dev.hygradle.dsl.plugin.LatePlugin>("extPlugin") {
                                manifest {
                                    mainClass = "com.example.ExtPlugin"
                                }
                            }
                        }
                    }
                """.stripIndent()
		][dsl]

		def externalSrc = projectDir.resolve("external/src/main/java/com/example").toFile()
		externalSrc.mkdirs()
		new File(externalSrc, "ExtPlugin.java") << """\
            package com.example;
            public class ExtPlugin {}
        """.stripIndent()

		when:
		def result = runner("printRunClasspath").build()

		then: 'external plugin classes are on the external classpath'
		result.output.readLines().any { line ->
			line.contains("CLASSPATH:") && line.contains("external") && line.contains("classes")
		}

		where:
		dsl << GradleDsl.values()
	}

	def "cross-project compileOnly resolves to classes directories (#dsl)"() {
		given:
		setupMultiProject(dsl as GradleDsl, "compileOnly")

		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, (dsl as GradleDsl).buildFileName) << [
			(GradleDsl.GROOVY): """
                    tasks.register('printCompileClasspath') {
                        inputs.files(configurations.named('pluginBPluginCompileClasspath'))
                        doLast {
                            inputs.files.each { println "RESOLVED: \${it}" }
                        }
                    }
                """.stripIndent(),
			(GradleDsl.KOTLIN): """
                    tasks.register("printCompileClasspath") {
                        inputs.files(configurations.named("pluginBPluginCompileClasspath"))
                        doLast {
                            inputs.files.forEach { println("RESOLVED: \$it") }
                        }
                    }
                """.stripIndent()
		][dsl]

		when:
		def result = runner(":pluginB:printCompileClasspath").build()

		then: 'resolved files include classes directory from pluginA'
		result.output.readLines().any { line ->
			line.contains("RESOLVED:") && line.contains("pluginA") && line.contains("classes")
		}

		where:
		dsl << GradleDsl.values()
	}
}
