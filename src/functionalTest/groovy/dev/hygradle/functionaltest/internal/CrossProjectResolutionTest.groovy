package dev.hygradle.functionaltest.internal

import dev.hygradle.functionaltest.FunctionalSpec
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

	private void setupMultiProject(String pluginBDependencies) {
		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")

		settingsFile << """\
			plugins { id("dev.hygradle.settings") }
			hygradle {
				hytale { version = "1.0.0" }
			}
			gradle.lifecycle.beforeProject {
				group = "com.example"
				version = "1.0.0"
			}
			include(":pluginA", ":pluginB")
		""".stripIndent()

		// pluginA — defines a Hygradle plugin
		def pluginADir = projectDir.resolve("pluginA").toFile()
		pluginADir.mkdirs()
		new File(pluginADir, "build.gradle.kts") << """\
			plugins { id("dev.hygradle") }
			repositories {
				maven { url = uri(rootDir.resolve("localRepo")) }
			}
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
		new File(pluginBDir, "build.gradle.kts") << """\
			plugins { id("dev.hygradle") }
			repositories {
				maven { url = uri(rootDir.resolve("localRepo")) }
			}
			hygradle {
				plugins {
					register<dev.hygradle.dsl.plugin.LatePlugin>("pluginB") {
						manifest {
							mainClass = "com.example.PluginB"
						}
						dependencies {
							${pluginBDependencies}
						}
					}
				}
			}
		""".stripIndent()

		// pluginB source
		def pluginBSrc = projectDir.resolve("pluginB/src/main/java/com/example").toFile()
		pluginBSrc.mkdirs()
		new File(pluginBSrc, "PluginB.java") << """\
			package com.example;
			public class PluginB {}
		""".stripIndent()
	}

	// --- runtimePlugin scope ---

	def "cross-project runtimePlugin resolves to classes directories, not JARs"() {
		given:
		setupMultiProject('runtimePlugin(project(":pluginA"), "pluginA")')

		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, "build.gradle.kts") << """
			tasks.register("printRuntimeClasspath") {
				inputs.files(configurations.named("_hygradle_plugin_pluginBRuntimeClasspath"))
				doLast {
					inputs.files.forEach { println("RESOLVED: ${'$'}it") }
				}
			}
		""".stripIndent()

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
	}

	def "cross-project runtimePlugin dependency auto-wired into consumer manifest"() {
		given:
		setupMultiProject('runtimePlugin(project(":pluginA"), "pluginA")')

		when:
		def result = runner(":pluginB:generatePluginBManifest").build()

		then: 'pluginB manifest contains auto-discovered dependency on pluginA'
		def manifestFile = projectDir.resolve("pluginB/build/hygradle/plugins/pluginB/manifest/manifest.json").toFile()
		manifestFile.exists()
		def manifest = new JsonSlurper().parse(manifestFile)
		manifest.Dependencies != null
		manifest.Dependencies['com.example:pluginA'] == '^1.0.0'
	}

	def "user-declared optional manifest dependency takes precedence over runtimePlugin auto-discovery"() {
		given:
		setupMultiProject('runtimePlugin(project(":pluginA"), "pluginA")')

		// Override pluginB build file to include an explicit optional manifest dependency
		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, "build.gradle.kts").text = """\
			plugins { id("dev.hygradle") }
			repositories {
				maven { url = uri(rootDir.resolve("localRepo")) }
			}
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
							runtimePlugin(project(":pluginA"), "pluginA")
						}
					}
				}
			}
		""".stripIndent()

		when:
		def result = runner(":pluginB:generatePluginBManifest").build()

		then: 'pluginA appears in OptionalDependencies, NOT in Dependencies'
		def manifestFile = projectDir.resolve("pluginB/build/hygradle/plugins/pluginB/manifest/manifest.json").toFile()
		manifestFile.exists()
		def manifest = new JsonSlurper().parse(manifestFile)
		manifest.OptionalDependencies != null
		manifest.OptionalDependencies['com.example:pluginA'] == '1.0.0'
		manifest.Dependencies == null || !manifest.Dependencies.containsKey('com.example:pluginA')
	}

	// --- runtimePlugin + compilePlugin scope (both compile and runtime) ---

	def "cross-project runtimePlugin and compilePlugin resolves to classes directories for both classpaths"() {
		given:
		setupMultiProject('''
			runtimePlugin(project(":pluginA"), "pluginA")
			compilePlugin(project(":pluginA"), "pluginA")
		'''.stripIndent().trim())

		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, "build.gradle.kts") << """
			tasks.register("printCompileClasspath") {
				inputs.files(configurations.named("_hygradle_plugin_pluginBCompileClasspath"))
				doLast {
					inputs.files.forEach { println("COMPILE: ${'$'}it") }
				}
			}
			tasks.register("printRuntimeClasspath") {
				inputs.files(configurations.named("_hygradle_plugin_pluginBRuntimeClasspath"))
				doLast {
					inputs.files.forEach { println("RUNTIME: ${'$'}it") }
				}
			}
		""".stripIndent()

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
	}

	def "cross-project runtimePlugin and compilePlugin auto-wires into consumer manifest"() {
		given:
		setupMultiProject('''
			runtimePlugin(project(":pluginA"), "pluginA")
			compilePlugin(project(":pluginA"), "pluginA")
		'''.stripIndent().trim())

		when:
		def result = runner(":pluginB:generatePluginBManifest").build()

		then: 'pluginB manifest contains auto-discovered dependency on pluginA'
		def manifestFile = projectDir.resolve("pluginB/build/hygradle/plugins/pluginB/manifest/manifest.json").toFile()
		manifestFile.exists()
		def manifest = new JsonSlurper().parse(manifestFile)
		manifest.Dependencies != null
		manifest.Dependencies['com.example:pluginA'] == '^1.0.0'
	}

	// --- compilePlugin scope (compile-only, optional in manifest) ---

	def "cross-project compilePlugin resolves to compile classpath but not runtime"() {
		given:
		setupMultiProject('compilePlugin(project(":pluginA"), "pluginA")')

		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, "build.gradle.kts") << """
			tasks.register("printCompileClasspath") {
				inputs.files(configurations.named("_hygradle_plugin_pluginBCompileClasspath"))
				doLast {
					inputs.files.forEach { println("COMPILE: ${'$'}it") }
				}
			}
			tasks.register("printRuntimeClasspath") {
				inputs.files(configurations.named("_hygradle_plugin_pluginBRuntimeClasspath"))
				doLast {
					inputs.files.forEach { println("RUNTIME: ${'$'}it") }
				}
			}
		""".stripIndent()

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
	}

	def "cross-project compilePlugin auto-wires as OptionalDependency in manifest"() {
		given:
		setupMultiProject('compilePlugin(project(":pluginA"), "pluginA")')

		when:
		def result = runner(":pluginB:generatePluginBManifest").build()

		then: 'pluginB manifest contains auto-discovered optional dependency on pluginA'
		def manifestFile = projectDir.resolve("pluginB/build/hygradle/plugins/pluginB/manifest/manifest.json").toFile()
		manifestFile.exists()
		def manifest = new JsonSlurper().parse(manifestFile)
		manifest.OptionalDependencies != null
		manifest.OptionalDependencies['com.example:pluginA'] == '^1.0.0'
		manifest.Dependencies == null || !manifest.Dependencies.containsKey('com.example:pluginA')
	}

	def "user-declared required manifest dependency takes precedence over compilePlugin auto-discovery"() {
		given:
		setupMultiProject('compilePlugin(project(":pluginA"), "pluginA")')

		// Override pluginB build file to add an explicit required manifest dependency for pluginA
		def pluginBDir = projectDir.resolve("pluginB").toFile()
		new File(pluginBDir, "build.gradle.kts").text = """\
			plugins { id("dev.hygradle") }
			repositories {
				maven { url = uri(rootDir.resolve("localRepo")) }
			}
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
							compilePlugin(project(":pluginA"), "pluginA")
						}
					}
				}
			}
		""".stripIndent()

		when:
		def result = runner(":pluginB:generatePluginBManifest").build()

		then: 'pluginA appears in Dependencies, NOT in OptionalDependencies'
		def manifestFile = projectDir.resolve("pluginB/build/hygradle/plugins/pluginB/manifest/manifest.json").toFile()
		manifestFile.exists()
		def manifest = new JsonSlurper().parse(manifestFile)
		manifest.Dependencies != null
		manifest.Dependencies['com.example:pluginA'] == '1.0.0'
		manifest.OptionalDependencies == null || !manifest.OptionalDependencies.containsKey('com.example:pluginA')
	}

	// --- Plugin dependency isolation from source set ---

	def "plugin dependency is not on source set runtimeClasspath"() {
		given:
		createStubMavenArtifact("com/example", "other", "1.0.0")
		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")

		settingsFile << """\
			plugins { id("dev.hygradle.settings") }
			hygradle {
				hytale { version = "1.0.0" }
			}
		""".stripIndent()

		buildFile << """\
			plugins { id("dev.hygradle") }
			repositories {
				maven { url = uri("localRepo") }
			}
			hygradle {
				plugins {
					register<dev.hygradle.dsl.plugin.LatePlugin>("myPlugin") {
						manifest {
							mainClass = "com.example.MyPlugin"
						}
						dependencies {
							runtimeOnly("com.example:other:1.0.0")
						}
					}
				}
			}
			tasks.register("printPluginRuntimeClasspath") {
				inputs.files(configurations.named("_hygradle_plugin_myPluginRuntimeClasspath"))
				doLast {
					inputs.files.forEach { println("PLUGIN_RUNTIME: ${'$'}it") }
				}
			}
			tasks.register("printSourceSetRuntimeClasspath") {
				inputs.files(configurations.named("runtimeClasspath"))
				doLast {
					inputs.files.forEach { println("SS_RUNTIME: ${'$'}it") }
				}
			}
		""".stripIndent()

		when:
		def result = runner("printPluginRuntimeClasspath", "printSourceSetRuntimeClasspath").build()

		then: 'plugin runtime classpath includes the dependency'
		result.output.readLines().any { line ->
			line.contains("PLUGIN_RUNTIME:") && line.contains("other-1.0.0.jar")
		}

		and: 'source set runtimeClasspath does NOT include the dependency'
		!result.output.readLines().any { line ->
			line.contains("SS_RUNTIME:") && line.contains("other-1.0.0.jar")
		}
	}

	// --- Global task deduplication ---

	def "global tasks exist on root project, not on subprojects"() {
		given:
		setupMultiProject('runtimePlugin(project(":pluginA"), "pluginA")')

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
	}
}
