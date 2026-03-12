package dev.hygradle.functionaltest.internal.task.run

import com.github.tomakehurst.wiremock.WireMockServer
import dev.hygradle.functionaltest.FunctionalSpec
import dev.hygradle.functionaltest.GradleDsl
import spock.lang.Shared
import spock.lang.TempDir

import java.nio.file.Path
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
        ["--stacktrace", "--gradle-user-home=" + gradleHome.toString()]
    }

    private void setupProject(GradleDsl dsl) {
        setupProject(dsl, false)
    }

    private void setupProject(GradleDsl dsl, boolean unknownPlugin) {
        settingsFile(dsl) << [
                (GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle.settings' }
                hygradle {
                    hytale { version = '1.0.0' }
                }
            """.stripIndent(),
                (GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle.settings") }
                hygradle {
                    hytale { version = "1.0.0" }
                }
            """.stripIndent()
        ][dsl]

        gradleProperties << """\
            hygradle.hytale.oauth.base=http://localhost:${wireMock.port()}
            hygradle.hytale.accounts.base=http://localhost:${wireMock.port()}
            hygradle.hytale.session.base=http://localhost:${wireMock.port()}
        """.stripIndent()

        buildFile(dsl) << [
                (GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle' }
                repositories {
                    maven { url = file('localRepo') }
                }
                hygradle {
                    ${unknownPlugin ? "runs.register('test') { includePlugins('nonExistent') }" : "runs.register('test')"}
                }
            """.stripIndent(),
                (GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle") }
                repositories {
                    maven { url = uri("localRepo") }
                }
                hygradle {
                    ${unknownPlugin ? 'runs.register("test") { includePlugins("nonExistent") }' : 'runs.register("test")'}
                }
            """.stripIndent()
        ][dsl]
    }

    def "startTestServer completes auth handshake and fails at process execution (#dsl)"() {
        given:
        setupProject(dsl as GradleDsl)
        stubProfileEndpoint('{"owner":"test","profiles":[{"uuid":"mock-uuid","username":"MockUser"}]}')
        stubSessionEndpoint()

        when:
        def result = runner("startTestServer").buildAndFail()

        then:
        result.output.contains("Creating session for user 'MockUser'")
        wireMock.verify(getRequestedFor(urlPathEqualTo("/my-account/get-profiles")))
        wireMock.verify(postRequestedFor(urlPathEqualTo("/game-session/new")))

        where:
        dsl << GradleDsl.values()
    }

    def "startTestServer fails when no profiles are available (#dsl)"() {
        given:
        setupProject(dsl as GradleDsl)
        stubProfileEndpoint('{"owner":"test","profiles":[]}')

        when:
        def result = runner("startTestServer").buildAndFail()

        then:
        !result.output.contains("Creating session")

        where:
        dsl << GradleDsl.values()
    }

    def "startTestServer fails when run references unknown plugin (#dsl)"() {
        given:
        setupProject(dsl as GradleDsl, true)

        when:
        def result = runner("startTestServer").buildAndFail()

        then:
        result.output.contains("references unknown plugin 'nonExistent'")

        where:
        dsl << GradleDsl.values()
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

    private File getTokenFile() { projectDir.resolve("build/hygradle/auth/auth.json").toFile() }

    private File getCachedBundleDir() { gradleHome.resolve("caches/hygradle/bundles").toFile() }

    private File getCachedBundle() { new File(cachedBundleDir, "RELEASE-1.0.0.zip") }

    private File getCachedAssetDir() { gradleHome.resolve("caches/hygradle/assets").toFile() }

    private File getCachedAsset() { new File(cachedAssetDir, "RELEASE-1.0.0.zip") }

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
