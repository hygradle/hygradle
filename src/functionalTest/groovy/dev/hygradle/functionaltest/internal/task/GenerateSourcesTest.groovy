package dev.hygradle.functionaltest.internal.task

import dev.hygradle.functionaltest.FunctionalSpec
import dev.hygradle.functionaltest.GradleDsl
import spock.lang.TempDir

import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class GenerateSourcesTest extends FunctionalSpec {

	@TempDir
	Path gradleHome

	@Override
	protected List<String> baseRunnerArgs() {
		[
			"--stacktrace",
			"--gradle-user-home=" + gradleHome.toString()
		]
	}

	def setup() {
		tokenFile.parentFile.mkdirs()
		tokenFile << '{"accessToken":"mock-access","refreshToken":"mock-refresh"}'
	}

	private void setupProject(GradleDsl dsl, boolean decompile) {
		settingsFile(dsl) << [
			(GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle.settings' }
                hygradle {
                    hytale {
                        version = '1.0.0'
                        decompile = ${decompile}
                    }
                }
            """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle.settings") }
                hygradle {
                    hytale {
                        version = "1.0.0"
                        decompile = ${decompile}
                    }
                }
            """.stripIndent()
		][dsl]

		gradleProperties << """\
            hygradle.hytale.oauth.base=http://localhost:0
            hygradle.hytale.accounts.base=http://localhost:0
            hygradle.hytale.session.base=http://localhost:0
        """.stripIndent()

		buildFile(dsl) << [
			(GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle' }
                repositories {
                    maven { url = file('localRepo') }
                }
            """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle") }
                repositories {
                    maven { url = uri("localRepo") }
                }
            """.stripIndent()
		][dsl]

		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")
		createVineflowerStubArtifact()
	}

	def "generateSources task is not registered when decompile is false (#dsl)"() {
		given:
		setupProject(dsl as GradleDsl, false)

		when:
		def result = runner("tasks", "--all").build()

		then:
		!result.output.readLines().any { it.trim().startsWith("generateSources") }

		where:
		dsl << GradleDsl.values()
	}

	def "generateSources task is registered when decompile is true (#dsl)"() {
		given:
		setupProject(dsl as GradleDsl, true)

		when:
		def result = runner("tasks", "--all").build()

		then:
		result.output.contains("generateSources")

		where:
		dsl << GradleDsl.values()
	}

	def "generateSources produces Maven repo layout with JAR, sources JAR, and POM (#dsl)"() {
		given:
		setupProject(dsl as GradleDsl, true)

		when:
		def result = runner("generateSources").build()

		then:
		result.output.contains("BUILD SUCCESSFUL")

		and: 'Maven repo layout directory exists with expected artifacts'
		def gavDir = decompiledCacheDir.resolve("com/hypixel/hytale/Server/1.0.0").toFile()
		gavDir.exists()
		new File(gavDir, "Server-1.0.0.jar").exists()
		new File(gavDir, "Server-1.0.0-sources.jar").exists()
		new File(gavDir, "Server-1.0.0.pom").exists()
		new File(gavDir, "Server-1.0.0.module").exists()

		and: 'POM contains correct coordinates and Gradle metadata marker'
		def pom = new File(gavDir, "Server-1.0.0.pom").text
		pom.contains("<groupId>com.hypixel.hytale</groupId>")
		pom.contains("<artifactId>Server</artifactId>")
		pom.contains("<version>1.0.0</version>")
		pom.contains("do_not_remove: published-with-gradle-metadata")

		and: 'Gradle Module Metadata declares sources variant'
		def moduleText = new File(gavDir, "Server-1.0.0.module").text
		moduleText.contains('"sourcesElements"')
		moduleText.contains('"org.gradle.docstype": "sources"')
		moduleText.contains('Server-1.0.0-sources.jar')

		and: 'sources JAR contains only hytale sources'
		def sourcesJar = new JarFile(new File(gavDir, "Server-1.0.0-sources.jar"))
		def entries = sourcesJar.entries().toList()*.name
		sourcesJar.close()
		entries.size() > 0
		entries.every { it.startsWith("com/hypixel/hytale/") }

		where:
		dsl << GradleDsl.values()
	}

	def "generateSources exits early on second invocation (#dsl)"() {
		given:
		setupProject(dsl as GradleDsl, true)
		runner("generateSources").build()

		when:
		def result = runner("generateSources").build()

		then:
		result.output.contains("BUILD SUCCESSFUL")

		where:
		dsl << GradleDsl.values()
	}

	def "generateSources succeeds on re-execution when resolved from decompiled cache (#dsl)"() {
		given: 'project with decompiled cache as first repo (mirrors real hytale() ordering)'
		def decompiledUri = decompiledCacheDir.toUri().toString()
		setupProjectWithDecompiledCache(dsl as GradleDsl, decompiledUri)

		and: 'first run populates the decompiled cache, resolving from localRepo'
		runner("generateSources").build()

		and: 'delete sources JAR so early-exit does not trigger, then delete localRepo to force resolution from the decompiled cache'
		gradleHome.resolve("caches/hygradle/decompiled/com/hypixel/hytale/Server/1.0.0/Server-1.0.0-sources.jar").toFile().delete()
		projectDir.resolve("localRepo/com/hypixel/hytale").toFile().deleteDir()

		when: 're-execute so hytaleClasspath resolves from the decompiled cache'
		def result = runner("generateSources").build()

		then:
		result.output.contains("BUILD SUCCESSFUL")

		where:
		dsl << GradleDsl.values()
	}

	private void setupProjectWithDecompiledCache(GradleDsl dsl, String decompiledUri) {
		settingsFile(dsl) << [
			(GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle.settings' }
                hygradle {
                    hytale {
                        version = '1.0.0'
                        decompile = true
                    }
                }
            """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle.settings") }
                hygradle {
                    hytale {
                        version = "1.0.0"
                        decompile = true
                    }
                }
            """.stripIndent()
		][dsl]

		gradleProperties << """\
            hygradle.hytale.oauth.base=http://localhost:0
            hygradle.hytale.accounts.base=http://localhost:0
            hygradle.hytale.session.base=http://localhost:0
        """.stripIndent()

		buildFile(dsl) << [
			(GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle' }
                repositories {
                    maven { url = '${decompiledUri}' }
                    maven { url = file('localRepo') }
                }
            """.stripIndent(),
			(GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle") }
                repositories {
                    maven { url = uri("${decompiledUri}") }
                    maven { url = uri("localRepo") }
                }
            """.stripIndent()
		][dsl]

		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")
		createVineflowerStubArtifact()
	}

	private void createStubMavenArtifact(String groupPath, String artifactId, String version) {
		def artifactDir = projectDir.resolve("localRepo/${groupPath}/${artifactId}/${version}").toFile()
		artifactDir.mkdirs()

		new File(artifactDir, "${artifactId}-${version}.jar").bytes = createStubServerJar()

		new File(artifactDir, "${artifactId}-${version}.pom").text = """\
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>${groupPath.replace('/', '.')}</groupId>
                <artifactId>${artifactId}</artifactId>
                <version>${version}</version>
            </project>
        """.stripIndent()
	}

	private void createVineflowerStubArtifact() {
		def artifactDir = projectDir.resolve("localRepo/org/vineflower/vineflower/1.11.1").toFile()
		artifactDir.mkdirs()

		new File(artifactDir, "vineflower-1.11.1.jar").bytes = createVineflowerStubJar()

		new File(artifactDir, "vineflower-1.11.1.pom").text = """\
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.vineflower</groupId>
                <artifactId>vineflower</artifactId>
                <version>1.11.1</version>
            </project>
        """.stripIndent()
	}

	/**
	 * Creates a JAR with a stub ConsoleDecompiler that copies the input file to the output dir.
	 * This simulates Vineflower's behavior without actually decompiling anything.
	 */
	private byte[] createVineflowerStubJar() {
		// Compile the stub class using javac
		def tmpDir = Files.createTempDirectory("vineflower-stub")
		def srcDir = tmpDir.resolve("org/jetbrains/java/decompiler/main/decompiler")
		Files.createDirectories(srcDir)

		def sourceFile = srcDir.resolve("ConsoleDecompiler.java")
		sourceFile.text = """\
            package org.jetbrains.java.decompiler.main.decompiler;
            import java.io.*;
            import java.nio.file.*;
            public class ConsoleDecompiler {
                public static void main(String[] args) throws Exception {
                    Path input = Paths.get(args[0]);
                    Path outputDir = Paths.get(args[1]);
                    Files.createDirectories(outputDir);
                    if (Files.isDirectory(input)) {
                        Files.walk(input).filter(Files::isRegularFile).forEach(source -> {
                            try {
                                Path target = outputDir.resolve(input.relativize(source));
                                Files.createDirectories(target.getParent());
                                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
                    } else {
                        Files.copy(input, outputDir.resolve(input.getFileName()),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        """.stripIndent()

		def compiler = ToolProvider.getSystemJavaCompiler()
		def fileManager = compiler.getStandardFileManager(null, null, null)
		def compilationUnits = fileManager.getJavaFileObjects(sourceFile.toFile())
		def task = compiler.getTask(null, fileManager, null, ["-d", tmpDir.toString()], null, compilationUnits)
		assert task.call(): "Failed to compile stub ConsoleDecompiler"
		fileManager.close()

		// Package into JAR
		def classFile = tmpDir.resolve("org/jetbrains/java/decompiler/main/decompiler/ConsoleDecompiler.class")
		def bytes = new ByteArrayOutputStream()
		def jar = new JarOutputStream(bytes)
		jar.putNextEntry(new JarEntry("org/jetbrains/java/decompiler/main/decompiler/ConsoleDecompiler.class"))
		jar.write(classFile.toFile().bytes)
		jar.closeEntry()
		jar.close()

		// Cleanup
		tmpDir.toFile().deleteDir()

		bytes.toByteArray()
	}

	private static byte[] createStubServerJar() {
		def bytes = new ByteArrayOutputStream()
		def jar = new JarOutputStream(bytes)
		jar.putNextEntry(new JarEntry("com/hypixel/hytale/Main.class"))
		jar.write(new byte[0])
		jar.closeEntry()
		jar.putNextEntry(new JarEntry("org/example/Other.class"))
		jar.write(new byte[0])
		jar.closeEntry()
		jar.close()
		bytes.toByteArray()
	}

	private File getTokenFile() {
		gradleHome.resolve("caches/hygradle/auth/auth.json").toFile()
	}

	private Path getDecompiledCacheDir() {
		gradleHome.resolve("caches/hygradle/decompiled")
	}
}
