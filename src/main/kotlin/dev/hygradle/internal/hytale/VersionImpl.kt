package dev.hygradle.internal.hytale

import dev.hygradle.dsl.hytale.Patchline
import dev.hygradle.dsl.hytale.Version

abstract class VersionImpl : Version {
  init {
    patchline.convention(Patchline.RELEASE)
  }
}
