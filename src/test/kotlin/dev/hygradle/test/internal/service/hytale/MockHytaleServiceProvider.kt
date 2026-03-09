package dev.hygradle.test.internal.service.hytale

import dev.hygradle.internal.service.hytale.HytaleService
import dev.hygradle.internal.service.hytale.HytaleServiceImpl
import dev.hygradle.internal.service.hytale.HytaleServiceProvider
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.auth.providers.*

class MockHytaleServiceProvider : HytaleServiceProvider {
  override fun create(loadExistingTokens: () -> BearerTokens): HytaleService =
      HytaleServiceImpl(MockHytaleServiceEngine, loadExistingTokens)
}

val MockHytaleServiceEngine = MockEngine { request -> respond("") }
