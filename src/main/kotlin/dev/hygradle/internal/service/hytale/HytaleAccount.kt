package dev.hygradle.internal.service.hytale

import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.providers.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.internal.cc.base.logger

abstract class HytaleAccount : BuildService<HytaleAccount.Parameters>, AutoCloseable {
  interface Parameters : BuildServiceParameters {
    val oauthBaseUrl: Property<String>
    val accountBaseUrl: Property<String>
    val sessionBaseUrl: Property<String>
    val tokenFile: RegularFileProperty
  }

  val service: HytaleService
    field =
        HytaleServiceImpl(
            CIO.create(),
            oauthBaseUrl = parameters.oauthBaseUrl.get(),
            accountBaseUrl = parameters.accountBaseUrl.get(),
            sessionBaseUrl = parameters.sessionBaseUrl.get(),
            logger,
            this::loadTokens,
        )

  @OptIn(ExperimentalSerializationApi::class)
  private fun loadTokens(): BearerTokens =
      parameters.tokenFile
          .get()
          .asFile
          .let { Json.decodeFromStream<SerializableBearerToken>(it.inputStream()) }
          .let { BearerTokens(it.accessToken, it.refreshToken) }

  override fun close() {
    if (service.tokens.isEmpty()) return

    parameters.tokenFile.get().asFile.also {
      it.parentFile.mkdirs()
      it.writeText(
          Json.encodeToString(
              SerializableBearerToken(
                  service.tokens.last().accessToken,
                  service.tokens.last().refreshToken!!,
              )
          )
      )
    }
  }

  companion object {
    const val OAUTH_BASE_PROPERTY = "hygradle.hytale.oauth.base"
    const val OAUTH_BASE = "https://oauth.accounts.hytale.com"

    const val ACCOUNT_BASE_PROPERTY = "hygradle.hytale.accounts.base"
    const val ACCOUNT_BASE = "https://account-data.hytale.com"

    const val SESSION_BASE_PROPERTY = "hygradle.hytale.session.base"
    const val SESSION_BASE = "https://sessions.hytale.com"
  }
}
