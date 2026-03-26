package dev.hygradle.functionaltest.internal

import dev.hygradle.functionaltest.FunctionalSpec
import groovy.json.JsonSlurper

import java.util.jar.JarOutputStream

class ManifestGenerationTest extends FunctionalSpec {

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

	private void setupSinglePlugin(String extraManifestConfig = '') {
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
			group = "com.example"
			version = "1.0.0"
			hygradle {
				plugins {
					register<dev.hygradle.dsl.plugin.LatePlugin>("testPlugin") {
						manifest {
							mainClass = "com.example.TestPlugin"
							${extraManifestConfig}
						}
					}
				}
			}
		""".stripIndent()

		def srcDir = projectDir.resolve("src/main/java/com/example").toFile()
		srcDir.mkdirs()
		new File(srcDir, "TestPlugin.java") << """\
			package com.example;
			public class TestPlugin {}
		""".stripIndent()
	}

	private Map parseManifest() {
		def manifestFile = projectDir.resolve("build/hygradle/plugins/testPlugin/manifest/manifest.json").toFile()
		assert manifestFile.exists(): "manifest.json was not generated"
		new JsonSlurper().parse(manifestFile) as Map
	}

	// --- Auto-detection scenarios ---

	def "IncludesAssetPack is true when root-level Server directory exists"() {
		given:
		setupSinglePlugin()
		def serverDir = projectDir.resolve("src/main/resources/Server").toFile()
		serverDir.mkdirs()
		new File(serverDir, "dummy.txt") << "content"

		when:
		runner("generateTestPluginManifest").build()

		then:
		parseManifest().IncludesAssetPack == true
	}

	def "IncludesAssetPack is true when root-level Common directory exists"() {
		given:
		setupSinglePlugin()
		def commonDir = projectDir.resolve("src/main/resources/Common").toFile()
		commonDir.mkdirs()
		new File(commonDir, "dummy.txt") << "content"

		when:
		runner("generateTestPluginManifest").build()

		then:
		parseManifest().IncludesAssetPack == true
	}

	def "IncludesAssetPack is false when no resource directories exist"() {
		given:
		setupSinglePlugin()

		when:
		runner("generateTestPluginManifest").build()

		then:
		parseManifest().IncludesAssetPack == false
	}

	def "IncludesAssetPack is false when asset directory is nested, not root-level"() {
		given:
		setupSinglePlugin()
		def nestedDir = projectDir.resolve("src/main/resources/sub/Common").toFile()
		nestedDir.mkdirs()
		new File(nestedDir, "dummy.txt") << "content"

		when:
		runner("generateTestPluginManifest").build()

		then:
		parseManifest().IncludesAssetPack == false
	}

	// --- User override scenarios ---

	def "explicit includesAssetPack true overrides auto-detection when no dirs present"() {
		given:
		setupSinglePlugin('includesAssetPack = true')

		when:
		runner("generateTestPluginManifest").build()

		then:
		parseManifest().IncludesAssetPack == true
	}

	def "explicit includesAssetPack false overrides auto-detection when dirs present"() {
		given:
		setupSinglePlugin('includesAssetPack = false')
		def serverDir = projectDir.resolve("src/main/resources/Server").toFile()
		serverDir.mkdirs()
		new File(serverDir, "dummy.txt") << "content"

		when:
		runner("generateTestPluginManifest").build()

		then:
		parseManifest().IncludesAssetPack == false
	}
}
