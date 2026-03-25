package dev.hygradle.internal.system

import dev.hygradle.internal.attributes.Category as HygradleCategory
import dev.hygradle.internal.attributes.Usage as HygradleUsage
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage

class AttributeSystem : Plugin<Project> {
  override fun apply(project: Project): Unit =
      with(project) {
        dependencies.attributesSchema {
          attribute(Usage.USAGE_ATTRIBUTE) {
            compatibilityRules.add(HygradleUsage.CompatibilityRule::class.java)
          }

          attribute(Category.CATEGORY_ATTRIBUTE) {
            compatibilityRules.add(HygradleCategory.CompatibilityRule::class.java)
          }
        }
      }
}
