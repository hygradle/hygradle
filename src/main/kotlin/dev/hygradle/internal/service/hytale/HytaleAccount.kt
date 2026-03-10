package dev.hygradle.internal.service.hytale

import dev.hygradle.internal.service.SerializableBearerToken
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.providers.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.slf4j.LoggerFactory

abstract class HytaleAccount : BuildService<HytaleAccount.Parameters> {
  private val logger = LoggerFactory.getLogger(HytaleAccount::class.java)

  interface Parameters : BuildServiceParameters {
    val oauthBaseUrl: Property<String>
    val accountBaseUrl: Property<String>
    val sessionBaseUrl: Property<String>
    val tokenFile: RegularFileProperty
  }

  val service: HytaleService =
      HytaleServiceImpl(
          CIO.create(),
          oauthBaseUrl = parameters.oauthBaseUrl.get(),
          accountBaseUrl = parameters.accountBaseUrl.get(),
          sessionBaseUrl = parameters.sessionBaseUrl.get(),
          this::loadTokens,
      )

  @OptIn(ExperimentalSerializationApi::class)
  private fun loadTokens(): BearerTokens =
      parameters.tokenFile
          .get()
          .asFile
          .let { Json.decodeFromStream<SerializableBearerToken>(it.inputStream()) }
          .let { BearerTokens(it.accessToken, it.refreshToken) }

  companion object {
    const val OAUTH_BASE_PROPERTY = "hygradle.hytale.oauth.base"
    const val OAUTH_BASE = "oauth.accounts.hytale.com"

    const val ACCOUNT_BASE_PROPERTY = "hygradle.hytale.accounts.base"
    const val ACCOUNT_BASE = "account-data.hytale.com"

    const val SESSION_BASE_PROPERTY = "hygradle.hytale.session.base"
    const val SESSION_BASE = "sessions.hytale.com"
  }
}
