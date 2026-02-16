import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

group = "dev.hygradle"

version = "0.0.1"

plugins {
  `kotlin-dsl`
  groovy
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.plugin.publish)
  alias(libs.plugins.shadow)
  alias(libs.plugins.spotless)
  alias(libs.plugins.dokka)
}

kotlin {
  @OptIn(ExperimentalAbiValidation::class) abiValidation { enabled = true }
  jvmToolchain(25)
  compilerOptions {
    allWarningsAsErrors = true
    apiVersion = KotlinVersion.KOTLIN_2_3
    languageVersion = apiVersion
    jvmTarget = JvmTarget.fromTarget("25")
  }
}

tasks.withType<ShadowJar> {
  archiveClassifier = null as String?
  minimizeJar = true
}

@Suppress("UnstableApiUsage")
testing.suites {
  val functionalTest by registering(JvmTestSuite::class) { useSpock() }
}

dependencies {
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.vineflower)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.de.undercouch.download)

  "functionalTestImplementation"(gradleTestKit())
}

spotless {
  kotlin { ktfmt(libs.versions.ktfmt.get()).metaStyle() }
  kotlinGradle { ktfmt(libs.versions.ktfmt.get()).metaStyle() }
}

gradlePlugin {
  vcsUrl = "https://github.com/remi-gelinas/hygradle"
  website = "https://hygradle.dev"
  plugins.register("dev.hygradle") { implementationClass = "dev.hygradle.HygradlePlugin" }
}
