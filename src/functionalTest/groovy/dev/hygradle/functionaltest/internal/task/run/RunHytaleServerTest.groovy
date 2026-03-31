package dev.hygradle.functionaltest.internal.task.run

import com.github.tomakehurst.wiremock.WireMockServer
import dev.hygradle.functionaltest.FunctionalSpec
import spock.lang.Shared
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import javax.tools.ToolProvider

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

class RunHytaleServerTest extends FunctionalSpec {

	@Shared
	WireMockServer wireMock

	@TempDir
	Path gradleHome

	def setupSpec() {
		wireMock = new WireMockServer(wireMockConfig().dynamicPort())
		wireMock.start()
	}

	def cleanupSpec() {
		wireMock.stop()
	}

	def cleanup() {
		wireMock.resetAll()
	}

	def setup() {
		tokenFile.parentFile.mkdirs()
		tokenFile << '{"accessToken":"mock-access","refreshToken":"mock-refresh"}'

		cachedBundleDir.mkdirs()
		cachedBundle.bytes = createBundleZip("placeholder-bundle".bytes)

		cachedAssetDir.mkdirs()
		cachedAsset.bytes = createBundleZip("placeholder-assets".bytes)

		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0")
		createStubMavenArtifact("org/hotswapagent", "hotswap-agent-core", "2.0.3")
		createStubMavenArtifact("dev/hygradle", "harness", "0.0.1")
	}

	@Override
	protected List<String> baseRunnerArgs() {
		[
			"--stacktrace",
			"--gradle-user-home=" + gradleHome.toString()
		]
	}

	private void setupProject() {
		setupProject(false)
	}

	private void setupProject(boolean unknownPlugin) {
		settingsFile << """\
			plugins { id("dev.hygradle.settings") }
			hygradle {
				hytale { version = "1.0.0" }
			}
		""".stripIndent()

		gradleProperties << """\
			hygradle.hytale.oauth.base=http://localhost:${wireMock.port()}
			hygradle.hytale.accounts.base=http://localhost:${wireMock.port()}
			hygradle.hytale.session.base=http://localhost:${wireMock.port()}
		""".stripIndent()

		buildFile << """\
			plugins { id("dev.hygradle") }
			repositories {
				maven { url = uri("localRepo") }
			}
			hygradle {
				${unknownPlugin
				? 'runs.register("test") { dependencies { runtimePlugin(project(), "nonExistent") } }'
				: 'runs.register("test")'}
			}
		""".stripIndent()
	}

	def "startTestServer completes auth handshake and fails at process execution"() {
		given:
		setupProject()
		stubProfileEndpoint('{"owner":"test","profiles":[{"uuid":"mock-uuid","username":"MockUser"}]}')
		stubSessionEndpoint()

		when:
		def result = runner("startTestServer").buildAndFail()

		then:
		result.output.contains("Creating session for user 'MockUser'")
		wireMock.verify(getRequestedFor(urlPathEqualTo("/my-account/get-profiles")))
		wireMock.verify(postRequestedFor(urlPathEqualTo("/game-session/new")))
	}

	def "startTestServer fails when no profiles are available"() {
		given:
		setupProject()
		stubProfileEndpoint('{"owner":"test","profiles":[]}')

		when:
		def result = runner("startTestServer").buildAndFail()

		then:
		!result.output.contains("Creating session")
	}

	def "startTestServer fails when run references unknown plugin"() {
		given:
		setupProject(true)

		when:
		def result = runner("startTestServer").buildAndFail()

		then:
		result.output.contains("Unable to find a variant of 'root project :' with the requested capability: coordinates 'dev.hygradle.plugin:nonExistent'")
	}

	def "extractedAssetsClasspath resolves to asset zip file in single project"() {
		given:
		setupProject()

		buildFile << """
			tasks.register("printAssets") {
				val assetFiles = files(configurations.named("_hygradle_hytaleAssetsClasspath"))
				inputs.files(assetFiles)
				doLast {
					assetFiles.forEach { println("ASSET: ${'$'}it") }
				}
			}
		""".stripIndent()

		when:
		def result = runner("printAssets").build()

		then: 'configuration resolves directly to the extracted asset zip file'
		result.output.readLines().any { line ->
			line.startsWith("ASSET:") && line.contains("caches/hygradle/assets") && line.endsWith("RELEASE-1.0.0.zip")
		}
	}

	def "startTestServer with decompile completes auth handshake after generateSources"() {
		given:
		setupProjectWithDecompile()
		stubProfileEndpoint('{"owner":"test","profiles":[{"uuid":"mock-uuid","username":"MockUser"}]}')
		stubSessionEndpoint()

		when:
		def result = runner("startTestServer").buildAndFail()

		then: 'generateSources ran successfully'
		result.output.contains(":generateSources")

		and: 'auth handshake completed (classpath was not corrupted by generateSources)'
		result.output.contains("Creating session for user 'MockUser'")
	}

	private void stubProfileEndpoint(String responseJson) {
		wireMock.stubFor(
				get(urlPathEqualTo("/my-account/get-profiles"))
				.willReturn(okJson(responseJson))
				)
	}

	private void stubSessionEndpoint() {
		wireMock.stubFor(
				post(urlPathEqualTo("/game-session/new"))
				.willReturn(okJson('{"sessionToken":"mock-session","identityToken":"mock-identity","expiresAt":"2099-01-01T00:00:00Z"}'))
				)
	}

	private File getTokenFile() {
		gradleHome.resolve("caches/hygradle/auth/auth.json").toFile()
	}

	private File getCachedBundleDir() {
		gradleHome.resolve("caches/hygradle/bundles").toFile()
	}

	private File getCachedBundle() {
		new File(cachedBundleDir, "RELEASE-1.0.0.zip")
	}

	private File getCachedAssetDir() {
		gradleHome.resolve("caches/hygradle/assets").toFile()
	}

	private File getCachedAsset() {
		new File(cachedAssetDir, "RELEASE-1.0.0.zip")
	}

	private void setupProjectWithDecompile() {
		settingsFile << """\
			plugins { id("dev.hygradle.settings") }
			hygradle {
				hytale {
					version = "1.0.0"
					decompile = true
				}
				vineflower { version = "0.0.0-stub" }
			}
		""".stripIndent()

		gradleProperties << """\
			hygradle.hytale.oauth.base=http://localhost:${wireMock.port()}
			hygradle.hytale.accounts.base=http://localhost:${wireMock.port()}
			hygradle.hytale.session.base=http://localhost:${wireMock.port()}
		""".stripIndent()

		buildFile << """\
			plugins { id("dev.hygradle") }
			repositories {
				maven { url = uri("localRepo") }
			}
			hygradle {
				runs.register("test")
			}
		""".stripIndent()

		createStubMavenArtifact("com/hypixel/hytale", "Server", "1.0.0", createStubServerJar())
		createStubMavenArtifact("org/hotswapagent", "hotswap-agent-core", "2.0.3", createStubJar())
		createStubMavenArtifact("dev/hygradle", "harness", "0.0.1", createStubJar())
		createVineflowerStubArtifact("0.0.0-stub")
	}

	private void createStubMavenArtifact(String groupPath, String artifactId, String version) {
		createStubMavenArtifact(groupPath, artifactId, version, createStubJar())
	}

	private void createStubMavenArtifact(String groupPath, String artifactId, String version, byte[] jar) {
		def artifactDir = projectDir.resolve("localRepo/${groupPath}/${artifactId}/${version}").toFile()
		artifactDir.mkdirs()

		new File(artifactDir, "${artifactId}-${version}.jar").bytes = jar

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

	private static byte[] createStubServerJar() {
		def bytes = new ByteArrayOutputStream()
		def jar = new JarOutputStream(bytes)
		jar.putNextEntry(new JarEntry("com/hypixel/hytale/Main.class"))
		jar.write(new byte[0])
		jar.closeEntry()
		jar.close()
		bytes.toByteArray()
	}

	private void createVineflowerStubArtifact(String version) {
		def artifactDir = projectDir.resolve("localRepo/org/vineflower/vineflower/${version}").toFile()
		artifactDir.mkdirs()

		new File(artifactDir, "vineflower-${version}.jar").bytes = createVineflowerStubJar()

		new File(artifactDir, "vineflower-${version}.pom").text = """\
			<project>
				<modelVersion>4.0.0</modelVersion>
				<groupId>org.vineflower</groupId>
				<artifactId>vineflower</artifactId>
				<version>${version}</version>
			</project>
		""".stripIndent()
	}

	private byte[] createVineflowerStubJar() {
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

		def classFile = tmpDir.resolve("org/jetbrains/java/decompiler/main/decompiler/ConsoleDecompiler.class")
		def bytes = new ByteArrayOutputStream()
		def jar = new JarOutputStream(bytes)
		jar.putNextEntry(new JarEntry("org/jetbrains/java/decompiler/main/decompiler/ConsoleDecompiler.class"))
		jar.write(classFile.toFile().bytes)
		jar.closeEntry()
		jar.close()

		tmpDir.toFile().deleteDir()

		bytes.toByteArray()
	}

	private static byte[] createBundleZip(byte[] content) {
		def bytes = new ByteArrayOutputStream()
		def zip = new ZipOutputStream(bytes)
		zip.putNextEntry(new ZipEntry("Assets.zip"))
		zip.write(content)
		zip.closeEntry()
		zip.close()
		bytes.toByteArray()
	}
}
