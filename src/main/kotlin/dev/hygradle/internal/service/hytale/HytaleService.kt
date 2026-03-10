package dev.hygradle.internal.service.hytale

import dev.hygradle.dsl.hytale.Patchline

interface HytaleService {
  fun getAssetBundle(patchline: Patchline, version: String): String

  fun getAvailableProfiles()

  fun createGameSession(uuid: String)

  fun terminateGameSession(token: String)
}
