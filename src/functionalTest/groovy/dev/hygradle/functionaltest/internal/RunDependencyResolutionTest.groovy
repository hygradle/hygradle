package dev.hygradle.functionaltest.internal

import dev.hygradle.functionaltest.FunctionalSpec

import java.util.jar.JarOutputStream

class RunDependencyResolutionTest extends FunctionalSpec {

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

	// --- Single-project setup ---

	private void setupSingleProject(String runDependencies) {
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
        """.stripIndent()

		buildFile << """\
            plugins { id("dev.hygradle") }
            repositories {
                maven { url = uri("localRepo") }
            }
            hygradle {
                plugins {
                    register<dev.hygradle.dsl.plugin.LatePlugin>("testPlugin") {
                        manifest {
                            mainClass = "com.example.TestPlugin"
                        }
                    }
                }
                runs {
                    register("dev") {
                        dependencies {
                            ${runDependencies}
                        }
                    }
                }
            }
        """.stripIndent()

		def pluginSrc = projectDir.resolve("src/main/java/com/example").toFile()
		pluginSrc.mkdirs()
		new File(pluginSrc, "TestPlugin.java") << """\
            package com.example;
            public class TestPlugin {}
        """.stripIndent()
	}

	// --- Multi-project setup ---

	private void setupMultiProject(String projectARunDependencies) {
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
            include(":projectA", ":projectB")
        """.stripIndent()

		// projectA — defines pluginA + a run
		def projectADir = projectDir.resolve("projectA").toFile()
		projectADir.mkdirs()
		new File(projectADir, "build.gradle.kts") << """\
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
                runs {
                    register("dev") {
                        dependencies {
                            ${projectARunDependencies}
                        }
                    }
                }
            }
        """.stripIndent()

		def pluginASrc = projectDir.resolve("projectA/src/main/java/com/example").toFile()
		pluginASrc.mkdirs()
		new File(pluginASrc, "PluginA.java") << """\
            package com.example;
            public class PluginA {}
        """.stripIndent()

		// projectB — defines pluginB (no run)
		def projectBDir = projectDir.resolve("projectB").toFile()
		projectBDir.mkdirs()
		new File(projectBDir, "build.gradle.kts") << """\
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
                    }
                }
            }
        """.stripIndent()

		def pluginBSrc = projectDir.resolve("projectB/src/main/java/com/example").toFile()
		pluginBSrc.mkdirs()
		new File(pluginBSrc, "PluginB.java") << """\
            package com.example;
            public class PluginB {}
        """.stripIndent()
	}

	// --- Task 7: Local plugin resolution ---

	def "runtimePlugin resolves local plugin classes on run classpath"() {
		given:
		setupSingleProject('runtimePlugin(project(), "testPlugin")')

		buildFile << """
            tasks.register("printRunClasspath") {
                inputs.files(configurations.named("_hygradle_run_devRuntimeClasspath"))
                doLast {
                    inputs.files.forEach { println("RUN_CP: ${'$'}it") }
                }
            }
        """.stripIndent()

		when:
		def result = runner("printRunClasspath").build()

		then: 'run classpath includes plugin classes directory'
		result.output.readLines().any { line ->
			line.contains("RUN_CP:") && line.contains("classes")
		}
	}

	// --- Task 8: Plain library on run ---

	def "runtimeOnly library appears on run classpath"() {
		given:
		createStubMavenArtifact("com/example", "some-lib", "2.0.0")
		setupSingleProject('''
            runtimePlugin(project(), "testPlugin")
            runtimeOnly("com.example:some-lib:2.0.0")
        '''.stripIndent().trim())

		buildFile << """
            tasks.register("printRunClasspath") {
                inputs.files(configurations.named("_hygradle_run_devRuntimeClasspath"))
                doLast {
                    inputs.files.forEach { println("RUN_CP: ${'$'}it") }
                }
            }
        """.stripIndent()

		when:
		def result = runner("printRunClasspath").build()

		then: 'run classpath includes the library JAR'
		result.output.readLines().any { line ->
			line.contains("RUN_CP:") && line.contains("some-lib-2.0.0.jar")
		}

		and: 'run classpath also includes plugin classes'
		result.output.readLines().any { line ->
			line.contains("RUN_CP:") && line.contains("classes")
		}
	}

	// --- Task 9: Cross-project plugin resolution ---

	def "cross-project runtimePlugin resolves sibling plugin classes on run classpath"() {
		given:
		setupMultiProject('''
            runtimePlugin(project(), "pluginA")
            runtimePlugin(project(":projectB"), "pluginB")
        '''.stripIndent().trim())

		def projectADir = projectDir.resolve("projectA").toFile()
		new File(projectADir, "build.gradle.kts") << """
            tasks.register("printRunClasspath") {
                inputs.files(configurations.named("_hygradle_run_devRuntimeClasspath"))
                doLast {
                    inputs.files.forEach { println("RUN_CP: ${'$'}it") }
                }
            }
        """.stripIndent()

		when:
		def result = runner(":projectA:printRunClasspath").build()

		then: 'run classpath includes pluginA classes'
		result.output.readLines().any { line ->
			line.contains("RUN_CP:") && line.contains("projectA") && line.contains("classes")
		}

		and: 'run classpath includes pluginB classes from sibling project'
		result.output.readLines().any { line ->
			line.contains("RUN_CP:") && line.contains("projectB") && line.contains("classes")
		}
	}

	// --- Task 10: Plugin assets resolution ---

	def "plugin assets resolve on run classpath via artifact view"() {
		given:
		setupSingleProject('runtimePlugin(project(), "testPlugin")')

		// Add an asset directory to trigger includesAssetPack
		def assetDir = projectDir.resolve("src/main/resources/Server").toFile()
		assetDir.mkdirs()
		new File(assetDir, "test-asset.txt") << "test"

		buildFile << """
            tasks.register("printRunAssets") {
                val assetFiles = configurations.named("_hygradle_run_devRuntimeClasspath").map {
                    it.incoming.artifactView {
                        attributes {
                            attribute(
                                org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE,
                                objects.named(org.gradle.api.attributes.Category::class.java, "hygradle-plugin-assets")
                            )
                        }
                        lenient(true)
                    }.files
                }
                inputs.files(assetFiles)
                doLast {
                    inputs.files.forEach { println("RUN_ASSET: ${'$'}it") }
                }
            }
        """.stripIndent()

		when:
		def result = runner("printRunAssets").build()

		then: 'run classpath includes assembled plugin assets'
		result.output.readLines().any { line ->
			line.contains("RUN_ASSET:") && line.contains("assets")
		}
	}

	// --- Task 11: Run with no dependencies ---

	def "run with no dependencies resolves without error"() {
		given:
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
                runs {
                    register("dev")
                }
            }
        """.stripIndent()

		buildFile << """
            tasks.register("printRunClasspath") {
                inputs.files(configurations.named("_hygradle_run_devRuntimeClasspath"))
                doLast {
                    inputs.files.forEach { println("RUN_CP: ${'$'}it") }
                }
            }
        """.stripIndent()

		when:
		def result = runner("printRunClasspath").build()

		then: 'resolves without error — empty classpath is fine'
		result.output.contains("BUILD SUCCESSFUL")
	}

	// --- Task 12: Transitive plugin assets ---

	def "transitive plugin assets resolve on run classpath"() {
		given:
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
            include(":projectA", ":projectB")
        """.stripIndent()

		// projectB — defines pluginB with assets
		def projectBDir = projectDir.resolve("projectB").toFile()
		projectBDir.mkdirs()
		new File(projectBDir, "build.gradle.kts") << """\
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
                    }
                }
            }
        """.stripIndent()

		def pluginBSrc = projectDir.resolve("projectB/src/main/java/com/example").toFile()
		pluginBSrc.mkdirs()
		new File(pluginBSrc, "PluginB.java") << """\
            package com.example;
            public class PluginB {}
        """.stripIndent()

		// pluginB has assets
		def pluginBAssets = projectDir.resolve("projectB/src/main/resources/Server").toFile()
		pluginBAssets.mkdirs()
		new File(pluginBAssets, "pluginB-asset.txt") << "pluginB asset"

		// projectA — defines pluginA which depends on pluginB, plus a run
		def projectADir = projectDir.resolve("projectA").toFile()
		projectADir.mkdirs()
		new File(projectADir, "build.gradle.kts") << """\
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
                        dependencies {
                            runtimePlugin(project(":projectB"), "pluginB")
                        }
                    }
                }
                runs {
                    register("dev") {
                        dependencies {
                            runtimePlugin(project(), "pluginA")
                        }
                    }
                }
            }
            tasks.register("printRunAssets") {
                val assetFiles = configurations.named("_hygradle_run_devRuntimeClasspath").map {
                    it.incoming.artifactView {
                        attributes {
                            attribute(
                                org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE,
                                objects.named(org.gradle.api.attributes.Category::class.java, "hygradle-plugin-assets")
                            )
                        }
                        lenient(true)
                    }.files
                }
                inputs.files(assetFiles)
                doLast {
                    inputs.files.forEach { println("RUN_ASSET: ${'$'}it") }
                }
            }
        """.stripIndent()

		def pluginASrc = projectDir.resolve("projectA/src/main/java/com/example").toFile()
		pluginASrc.mkdirs()
		new File(pluginASrc, "PluginA.java") << """\
            package com.example;
            public class PluginA {}
        """.stripIndent()

		when:
		def result = runner(":projectA:printRunAssets").build()

		then: 'run classpath includes pluginB assets transitively through pluginA dependency'
		result.output.readLines().any { line ->
			line.contains("RUN_ASSET:") && line.contains("projectB") && line.contains("assets")
		}
	}
}
