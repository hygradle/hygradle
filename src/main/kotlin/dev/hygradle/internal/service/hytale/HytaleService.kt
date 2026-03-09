package dev.hygradle.internal.service.hytale

interface HytaleService {
  fun getAssetBundle(patchline: String, version: String)

  fun getAvailableProfiles()

  fun createGameSession(uuid: String)

  fun terminateGameSession(token: String)
}
