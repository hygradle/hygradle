@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
  groovy
  alias(libs.plugins.kotlin)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.plugin.publish)
  alias(libs.plugins.shadow)
  alias(libs.plugins.spotless)
  alias(libs.plugins.dokka)
}

group = "dev.hygradle"

version = "0.0.1"

kotlin {
  explicitApi()
  @OptIn(ExperimentalAbiValidation::class) abiValidation { enabled = true }
  jvmToolchain(25)
  compilerOptions {
    allWarningsAsErrors = true
    apiVersion = KotlinVersion.KOTLIN_2_2
    languageVersion = apiVersion
    jvmTarget = JvmTarget.fromTarget("25")
  }
}

val shade: Configuration by configurations.creating

configurations.implementation.configure { extendsFrom(shade) }

val shadowJar by
    tasks.existing(ShadowJar::class) {
      archiveClassifier = null as String?
      configurations = listOf(shade)
      minimizeJar = true
    }

spotless {
  kotlin { ktfmt(libs.versions.ktfmt.get()).metaStyle() }
  kotlinGradle { ktfmt(libs.versions.ktfmt.get()).metaStyle() }
}

dependencies {
  compileOnly(libs.kotlin.gradle.plugin)

  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.vineflower)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.de.undercouch.download)
}

gradlePlugin {
  vcsUrl = "https://github.com/remi-gelinas/hygradle"
  website = "https://hygradle.dev"

  plugins.create("hygradle") {
    id = "dev.hygradle"
    displayName = "Hygradle"
    description = "Gradle plugin for Hytale plugin development."
    tags = listOf("hytale")
    implementationClass = "dev.hygradle.internal.HygradlePlugin"
  }
}

testing.suites {
  register<JvmTestSuite>("functionalTest") { dependencies.implementation(libs.gradle.testkit) }

  withType<JvmTestSuite>().configureEach { useJUnitJupiter(libs.versions.junit.get()) }
}
