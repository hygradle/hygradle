package dev.hygradle.internal.attributes

import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.CompatibilityCheckDetails

object Category {
  const val HYGRADLE = "hygradle"
  const val PLUGIN_MANIFEST = "hygradle-plugin-manifest"
  const val PLUGIN_ASSETS = "hygradle-plugin-assets"
  const val HYTALE_ASSETS = "hygradle-hytale-assets"

  class CompatibilityRule : AttributeCompatibilityRule<Category> {
    override fun execute(details: CompatibilityCheckDetails<Category>) {
      val consumer = details.consumerValue?.name
      val producer = details.producerValue?.name

      when (consumer) {
        HYGRADLE if producer == Category.LIBRARY -> {
          details.compatible()
        }
      }
    }
  }
}
