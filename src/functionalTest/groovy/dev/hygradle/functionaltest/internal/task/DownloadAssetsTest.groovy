package dev.hygradle.functionaltest.internal.task

import com.github.tomakehurst.wiremock.WireMockServer
import dev.hygradle.functionaltest.FunctionalSpec
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

	static final byte[] PLACEHOLDER_ASSETS_ZIP = [
		(byte) 0x50,
		(byte) 0x4B,
		(byte) 0x03,
		(byte) 0x04,
		(byte) 0x00
	] as byte[]

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
		[
			"--stacktrace",
			"--gradle-user-home=" + gradleHome.toString()
		]
	}

	private void setupProject() {
		settingsFile << """\
			plugins { id("dev.hygradle.settings") }
			hygradle {
				hytale {
					version = "1.0.0"
				}
			}
		""".stripIndent()

		gradleProperties << """\
			hygradle.hytale.oauth.base=http://localhost:${wireMock.port()}
			hygradle.hytale.accounts.base=http://localhost:${wireMock.port()}
			hygradle.hytale.session.base=http://localhost:${wireMock.port()}
		""".stripIndent()

		buildFile << """\
			plugins { id("dev.hygradle") }
		""".stripIndent()
	}

	def "downloadAssets downloads asset bundle to cache directory"() {
		given:
		setupProject()
		stubAssetBundleEndpoint()
		stubCdnDownload()

		when:
		def result = runner("downloadAssets").build()

		then:
		result.output.contains("BUILD SUCCESSFUL")
		cachedBundle.exists()
		cachedBundle.bytes == PLACEHOLDER_ASSETS_ZIP
	}

	def "downloadAssets skips download when bundle is already cached"() {
		given:
		setupProject()
		def bundleDir = cachedBundleDir
		bundleDir.mkdirs()
		cachedBundle << PLACEHOLDER_ASSETS_ZIP

		when:
		def result = runner("downloadAssets").build()

		then:
		result.output.contains("BUILD SUCCESSFUL")
		wireMock.findAll(getRequestedFor(urlPathMatching(".*"))).size() == 0
	}

	def "downloadAssets cleans stale bundles from cache"() {
		given:
		setupProject()
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

	private File getTokenFile() {
		gradleHome.resolve("caches/hygradle/auth/auth.json").toFile()
	}

	private File getCachedBundleDir() {
		gradleHome.resolve("caches/hygradle/bundles").toFile()
	}

	private File getCachedBundle() {
		new File(cachedBundleDir, "RELEASE-1.0.0.zip")
	}
}
