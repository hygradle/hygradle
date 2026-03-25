package dev.hygradle.internal.attributes

import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.api.attributes.Usage

object Usage {
  const val RUNTIME = "hygradle-runtime"
  const val COMPILE = "hygradle-compile"

  class CompatibilityRule : AttributeCompatibilityRule<Usage> {
    override fun execute(details: CompatibilityCheckDetails<Usage>) {
      val consumer = details.consumerValue?.name
      val producer = details.producerValue?.name

      when (consumer) {
        RUNTIME if producer == Usage.JAVA_RUNTIME -> {
          details.compatible()
        }
        COMPILE if producer == Usage.JAVA_API -> {
          details.compatible()
        }
      }
    }
  }
}
