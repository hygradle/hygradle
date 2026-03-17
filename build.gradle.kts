@file:Suppress("UnstableApiUsage")

import com.diffplug.gradle.spotless.SpotlessTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
  `kotlin-dsl`
  groovy
  `maven-publish`
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.spotless)
  alias(libs.plugins.dokka)
  alias(libs.plugins.dokka.javadoc)
}

kotlin {
  @OptIn(ExperimentalAbiValidation::class) abiValidation { enabled = true }
  jvmToolchain(21)
  compilerOptions {
    allWarningsAsErrors = true
    apiVersion = KotlinVersion.KOTLIN_2_3
    languageVersion = apiVersion
    jvmTarget = JvmTarget.JVM_21
    freeCompilerArgs.add("-Xexplicit-backing-fields")
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
  withSourcesJar()
}

tasks.javadoc { enabled = false }

val javadocJar by
    tasks.registering(Jar::class) {
      archiveClassifier = "javadoc"
      from(tasks.named("dokkaGeneratePublicationJavadoc"))
    }

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

tasks.withType<SpotlessTask> {
  notCompatibleWithConfigurationCache("https://github.com/diffplug/spotless/issues/2878")
}

dependencies {
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.core)
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
  testSourceSets.add(sourceSets["functionalTest"])

  plugins {
    register("dev.hygradle") {
      displayName = "Hygradle"
      description = "A mod development environment for Hytale."
      implementationClass = "dev.hygradle.internal.HygradlePlugin"
    }

    register("dev.hygradle.settings") {
      displayName = "Hygradle (Settings)"
      description = "A mod development environment for Hytale."
      implementationClass = "dev.hygradle.internal.HygradleSettingsPlugin"
    }
  }
}

publishing {
  repositories {
    maven {
      name = "hygradle"
      url = uri("https://maven.hygradle.dev")

      credentials(HttpHeaderCredentials::class) {
        name = "Authorization"
        value = providers.gradleProperty("hygradlePublishToken").map { "Bearer $it" }.getOrElse("")
      }

      authentication { create<HttpHeaderAuthentication>("header") }
    }
  }

  publications.withType<MavenPublication>().configureEach {
    if (name == "pluginMaven") {
      artifact(javadocJar)
    }
  }
}
