package dev.hygradle.internal

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails

enum class HygradleVariant {
  COMPILE,
  RUNTIME,
}

object HygradleAttributes {
  val VARIANT_ATTRIBUTE: Attribute<HygradleVariant> =
      Attribute.of("dev.hygradle.variant", HygradleVariant::class.java)

  val PLUGIN_NAME_ATTRIBUTE: Attribute<String> =
      Attribute.of("dev.hygradle.plugin.name", String::class.java)

  val PLUGIN_BUNDLE_ATTRIBUTE: Attribute<Boolean> =
      Attribute.of("dev.hygradle.plugin.bundle", Boolean::class.javaObjectType)

  const val PLUGIN_MANIFEST_ARTIFACT_TYPE = "hygradle-plugin-manifest"

  /**
   * When the consumer does not request [PLUGIN_BUNDLE_ATTRIBUTE], prefer individual plugin variants
   * (bundle = false) over the aggregate (bundle = true).
   */
  internal class PreferNonBundleDisambiguation : AttributeDisambiguationRule<Boolean> {
    override fun execute(details: MultipleCandidatesDetails<Boolean>) {
      if (details.consumerValue == null) {
        details.closestMatch(false)
      }
    }
  }
}
