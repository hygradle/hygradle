package dev.hygradle.functionaltest.internal.task

import dev.hygradle.functionaltest.FunctionalSpec

import java.nio.file.Files
import java.util.jar.JarOutputStream

class AssembleAssetsTest extends FunctionalSpec {

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

	private void setupSinglePlugin() {
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

	private File assetDir() {
		projectDir.resolve("build/hygradle/plugins/testPlugin/assets").toFile()
	}

	def "assembles root-level resource files as symlinks"() {
		given:
		setupSinglePlugin()
		def resourceDir = projectDir.resolve("src/main/resources").toFile()
		resourceDir.mkdirs()
		new File(resourceDir, "default.yml") << "key: value"

		when:
		runner("assembleTestPluginAssets").build()

		then:
		def symlink = assetDir().toPath().resolve("default.yml")
		Files.isSymbolicLink(symlink)
		symlink.toFile().text == "key: value"
	}

	def "assembles root-level resource directories as symlinks"() {
		given:
		setupSinglePlugin()
		def metaInfDir = projectDir.resolve("src/main/resources/META-INF/services").toFile()
		metaInfDir.mkdirs()
		new File(metaInfDir, "java.sql.Driver") << "org.h2.Driver"

		when:
		runner("assembleTestPluginAssets").build()

		then:
		def symlink = assetDir().toPath().resolve("META-INF")
		Files.isSymbolicLink(symlink)
		new File(symlink.toFile(), "services/java.sql.Driver").text == "org.h2.Driver"
	}

	def "assembles asset pack directories alongside other resources"() {
		given:
		setupSinglePlugin()
		def resourceDir = projectDir.resolve("src/main/resources").toFile()

		def serverDir = new File(resourceDir, "Server")
		serverDir.mkdirs()
		new File(serverDir, "dummy.txt") << "content"

		new File(resourceDir, "default.yml") << "key: value"

		when:
		runner("assembleTestPluginAssets").build()

		then:
		def assets = assetDir()
		Files.isSymbolicLink(assets.toPath().resolve("Server"))
		Files.isSymbolicLink(assets.toPath().resolve("default.yml"))
		Files.isSymbolicLink(assets.toPath().resolve("manifest.json"))
	}

	def "does not duplicate manifest from resource sources"() {
		given:
		setupSinglePlugin()
		def resourceDir = projectDir.resolve("src/main/resources").toFile()
		resourceDir.mkdirs()
		new File(resourceDir, "default.yml") << "key: value"

		when:
		runner("assembleTestPluginAssets").build()

		then: "manifest.json exists exactly once as a symlink to the generated manifest"
		def manifestSymlink = assetDir().toPath().resolve("manifest.json")
		Files.isSymbolicLink(manifestSymlink)
		def target = Files.readSymbolicLink(manifestSymlink)
		target.toString().contains("build/hygradle/plugins/testPlugin/manifest")
	}
}
