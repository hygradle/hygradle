package dev.hygradle.internal.service.hytale

import dev.hygradle.dsl.hytale.Patchline

interface HytaleService {
  fun getAssetBundle(patchline: Patchline, version: String): String

  fun getAvailableProfiles(): List<Profile>

  fun createGameSession(uuid: String): SessionTokens

  fun terminateGameSession(token: String)
}
