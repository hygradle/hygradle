package dev.hygradle.functionaltest.internal.task

import dev.hygradle.functionaltest.FunctionalSpec
import dev.hygradle.functionaltest.GradleDsl
import spock.lang.TempDir

import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExtractAssetsTest extends FunctionalSpec {

    @TempDir
    Path gradleHome

    static final byte[] INNER_ASSETS_CONTENT = "placeholder-assets".bytes

    def setup() {
        tokenFile.parentFile.mkdirs()
        tokenFile << '{"accessToken":"mock-access","refreshToken":"mock-refresh"}'

        cachedBundleDir.mkdirs()
        cachedBundle.bytes = createBundleZip(INNER_ASSETS_CONTENT)
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
            hygradle.hytale.oauth.base=http://localhost:0
            hygradle.hytale.accounts.base=http://localhost:0
            hygradle.hytale.session.base=http://localhost:0
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

    def "extractAssets extracts inner Assets.zip from bundle to asset cache (#dsl)"() {
        given:
        setupProject(dsl)

        when:
        def result = runner("extractAssets").build()

        then:
        result.output.contains("BUILD SUCCESSFUL")
        cachedAsset.exists()
        cachedAsset.bytes == INNER_ASSETS_CONTENT

        where:
        dsl << GradleDsl.values()
    }

    def "extractAssets skips extraction when assets are already cached (#dsl)"() {
        given:
        setupProject(dsl)
        def preExisting = "already-cached".bytes
        cachedAssetDir.mkdirs()
        cachedAsset.bytes = preExisting

        when:
        def result = runner("extractAssets").build()

        then:
        result.output.contains("BUILD SUCCESSFUL")
        cachedAsset.bytes == preExisting

        where:
        dsl << GradleDsl.values()
    }

    def "extractAssets cleans stale assets from cache (#dsl)"() {
        given:
        setupProject(dsl)
        cachedAssetDir.mkdirs()
        def staleAsset = new File(cachedAssetDir, "RELEASE-0.9.0.zip")
        staleAsset << "stale"

        when:
        def result = runner("extractAssets").build()

        then:
        result.output.contains("BUILD SUCCESSFUL")
        !staleAsset.exists()
        cachedAsset.exists()
        cachedAsset.bytes == INNER_ASSETS_CONTENT

        where:
        dsl << GradleDsl.values()
    }

    private static byte[] createBundleZip(byte[] assetsContent) {
        def bytes = new ByteArrayOutputStream()
        def zip = new ZipOutputStream(bytes)
        zip.putNextEntry(new ZipEntry("Assets.zip"))
        zip.write(assetsContent)
        zip.closeEntry()
        zip.close()
        bytes.toByteArray()
    }

    private File getTokenFile() { projectDir.resolve("build/hygradle/auth/auth.json").toFile() }

    private File getCachedBundleDir() { gradleHome.resolve("caches/hygradle/bundles").toFile() }

    private File getCachedBundle() { new File(cachedBundleDir, "RELEASE-1.0.0.zip") }

    private File getCachedAssetDir() { gradleHome.resolve("caches/hygradle/assets").toFile() }

    private File getCachedAsset() { new File(cachedAssetDir, "RELEASE-1.0.0.zip") }
}
