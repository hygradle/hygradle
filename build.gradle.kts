@file:Suppress("UnstableApiUsage", "Unused")

import com.diffplug.gradle.spotless.SpotlessTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
  `kotlin-dsl`
  groovy
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.plugin.publish)
  alias(libs.plugins.shadow)
  alias(libs.plugins.spotless)
  alias(libs.plugins.dokka)
  alias(libs.plugins.git.version)
}

group = "dev.hygradle"

version = "0.0.1"

kotlin {
  @OptIn(ExperimentalAbiValidation::class) abiValidation { enabled = true }
  jvmToolchain(25)
  compilerOptions {
    allWarningsAsErrors = true
    apiVersion = KotlinVersion.KOTLIN_2_3
    languageVersion = apiVersion
    jvmTarget = JvmTarget.fromTarget("25")
    freeCompilerArgs.add("-Xexplicit-backing-fields")
  }
}

tasks.withType<ShadowJar> { archiveClassifier = null as String? }

testing.suites {
  val test by
      getting(JvmTestSuite::class) {
        useSpock()
        dependencies {
          implementation(libs.ktor.client.mock)
          implementation(libs.junit.jupiter)
        }
      }

  val functionalTest by
      registering(JvmTestSuite::class) {
        useSpock()
        dependencies {
          implementation(gradleTestKit())
          implementation(libs.wiremock)
        }
      }
}

tasks.withType<SpotlessTask>() {
  notCompatibleWithConfigurationCache("https://github.com/diffplug/spotless/issues/2878")
}

dependencies {
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.foojay.resolver.convention)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.auth)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
}

spotless {
  kotlin { ktfmt(libs.versions.ktfmt.get()).metaStyle() }
  kotlinGradle { ktfmt(libs.versions.ktfmt.get()).metaStyle() }
  groovy { excludeJava() }
}

tasks.validatePlugins { enableStricterValidation = true }

gradlePlugin {
  vcsUrl = "https://github.com/remi-gelinas/hygradle"
  website = "https://hygradle.dev"

  testSourceSets.add(sourceSets["functionalTest"])

  plugins {
    register("dev.hygradle") {
      displayName = "Hygradle"
      description = "A mod development environment for Hytale."
      tags = listOf("hytale, gradle")
      implementationClass = "dev.hygradle.internal.HygradlePlugin"

      compatibility { features { configurationCache = true } }
    }

    register("dev.hygradle.settings") {
      displayName = "Hygradle (Settings)"
      description = "A mod development environment for Hytale."
      tags = listOf("hytale, gradle")
      implementationClass = "dev.hygradle.internal.HygradleSettingsPlugin"

      compatibility { features { configurationCache = true } }
    }
  }
}
