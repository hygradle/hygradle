@file:Suppress("UnstableApiUsage")

package dev.hygradle.internal

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.GradleInternal
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.getByType
import org.gradle.toolchains.foojay.FoojayToolchainsPlugin

class ConventionPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        (gradle as GradleInternal).settings.plugins.apply(FoojayToolchainsPlugin::class.java)

        plugins.apply(JavaPlugin::class.java)

        extensions.getByType<JavaPluginExtension>().toolchain {
          vendor.set(JvmVendorSpec.JETBRAINS)
          languageVersion.set(JavaLanguageVersion.of(25))
        }
      }
}
