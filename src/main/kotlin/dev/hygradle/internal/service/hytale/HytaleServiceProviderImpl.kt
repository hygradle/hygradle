package dev.hygradle.internal.service.hytale

import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.providers.*

class HytaleServiceProviderImpl : HytaleServiceProvider {
  override fun create(loadExistingTokens: () -> BearerTokens): HytaleService =
      HytaleServiceImpl(CIO.create(), loadExistingTokens)
}
