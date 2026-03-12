package dev.hygradle.functionaltest.internal.task

import com.github.tomakehurst.wiremock.WireMockServer
import dev.hygradle.functionaltest.FunctionalSpec
import dev.hygradle.functionaltest.GradleDsl
import spock.lang.Shared
import spock.lang.TempDir

import java.nio.file.Path

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

class DownloadAssetsTest extends FunctionalSpec {

    @Shared
    WireMockServer wireMock

    @TempDir
    Path gradleHome

    static final byte[] PLACEHOLDER_ASSETS_ZIP = [(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04, (byte) 0x00] as byte[]

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
    }

    @Override
    protected List<String> baseRunnerArgs() {
        ["--stacktrace", "--gradle-user-home=" + gradleHome.toString()]
    }

    private void setupProject(GradleDsl dsl) {
        settingsFile(dsl) << [
            (GradleDsl.GROOVY): """\
                plugins { id 'dev.hygradle.settings' }
                hygradle {
                    hytale {
                        version = '1.0.0'
                    }
                }
            """.stripIndent(),
            (GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle.settings") }
                hygradle {
                    hytale {
                        version = "1.0.0"
                    }
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
            """.stripIndent(),
            (GradleDsl.KOTLIN): """\
                plugins { id("dev.hygradle") }
            """.stripIndent()
        ][dsl]
    }

    def "downloadAssets downloads asset bundle to cache directory (#dsl)"() {
        given:
        setupProject(dsl as GradleDsl)
        stubAssetBundleEndpoint()
        stubCdnDownload()

        when:
        def result = runner("downloadAssets").build()

        then:
        result.output.contains("BUILD SUCCESSFUL")
        cachedBundle.exists()
        cachedBundle.bytes == PLACEHOLDER_ASSETS_ZIP

        where:
        dsl << GradleDsl.values()
    }

    def "downloadAssets skips download when bundle is already cached (#dsl)"() {
        given:
        setupProject(dsl as GradleDsl)
        def bundleDir = cachedBundleDir
        bundleDir.mkdirs()
        cachedBundle << PLACEHOLDER_ASSETS_ZIP

        when:
        def result = runner("downloadAssets").build()

        then:
        result.output.contains("BUILD SUCCESSFUL")
        wireMock.findAll(getRequestedFor(urlPathMatching(".*"))).size() == 0

        where:
        dsl << GradleDsl.values()
    }

    def "downloadAssets cleans stale bundles from cache (#dsl)"() {
        given:
        setupProject(dsl as GradleDsl)
        def bundleDir = cachedBundleDir
        bundleDir.mkdirs()
        def staleBundle = new File(bundleDir, "RELEASE-0.9.0.zip")
        staleBundle << "stale"

        stubAssetBundleEndpoint()
        stubCdnDownload()

        when:
        def result = runner("downloadAssets").build()

        then:
        result.output.contains("BUILD SUCCESSFUL")
        !staleBundle.exists()
        cachedBundle.exists()
        cachedBundle.bytes == PLACEHOLDER_ASSETS_ZIP

        where:
        dsl << GradleDsl.values()
    }

    private void stubAssetBundleEndpoint() {
        wireMock.stubFor(
                get(urlPathEqualTo("/game-assets/builds/release/1.0.0.zip"))
                        .willReturn(okJson('{"url":"http://localhost:' + wireMock.port() + '/cdn/bundle.zip"}'))
        )
    }

    private void stubCdnDownload() {
        wireMock.stubFor(
                get(urlPathEqualTo("/cdn/bundle.zip"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody(PLACEHOLDER_ASSETS_ZIP))
        )
    }

    private File getTokenFile() { projectDir.resolve("build/hygradle/auth/auth.json").toFile() }

    private File getCachedBundleDir() { gradleHome.resolve("caches/hygradle/bundles").toFile() }

    private File getCachedBundle() { new File(cachedBundleDir, "RELEASE-1.0.0.zip") }
}
