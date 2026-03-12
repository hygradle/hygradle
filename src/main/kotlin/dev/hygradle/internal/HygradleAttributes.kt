package dev.hygradle.internal

import org.gradle.api.attributes.Attribute

enum class HygradleVariant {
  COMPILE,
  RUNTIME,
}

object HygradleAttributes {
  val VARIANT_ATTRIBUTE: Attribute<HygradleVariant> =
      Attribute.of("dev.hygradle.variant", HygradleVariant::class.java)

  val PLUGIN_NAME_ATTRIBUTE: Attribute<String> =
      Attribute.of("dev.hygradle.plugin.name", String::class.java)

  const val PLUGIN_MANIFEST_ARTIFACT_TYPE = "hygradle-plugin-manifest"
}
