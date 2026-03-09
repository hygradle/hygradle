package dev.hygradle.internal.service.hytale

import io.ktor.client.plugins.auth.providers.*

interface HytaleServiceProvider {
  fun create(loadExistingTokens: () -> BearerTokens): HytaleService
}
