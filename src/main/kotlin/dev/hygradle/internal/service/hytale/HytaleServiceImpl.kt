package dev.hygradle.internal.service.hytale

import dev.hygradle.internal.service.AssetBundleResponse
import dev.hygradle.internal.service.DeviceCodeResponse
import dev.hygradle.internal.service.GetProfileResponse
import dev.hygradle.internal.service.TokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class HytaleServiceImpl(
    engine: HttpClientEngine,
    tokenLoader: suspend () -> BearerTokens?,
) : HytaleService {
  val tokens = mutableListOf<BearerTokens>()

  val client =
      HttpClient(engine) {
        install(ContentNegotiation) { json() }

        install(Auth) {
          reAuthorizeOnResponse {
            it.status == HttpStatusCode.Unauthorized ||
                // TODO: Remove this when Hypixel sends real responses
                (it.request.url.host == "account-data.hytale.com" &&
                    it.status == HttpStatusCode.Forbidden)
          }

          bearer {
            loadTokens {
              tokenLoader()?.let {
                tokens.add(it)
                tokens.last()
              }
            }

            refreshTokens {
              refreshOrRenewToken(oldTokens).let {
                tokens.add(it)
                tokens.last()
              }
            }
          }
        }
      }

  suspend fun refreshOrRenewToken(oldTokens: BearerTokens?): BearerTokens {
    if (oldTokens == null) return startDeviceFlow()

    return try {
      refreshToken(oldTokens)
    } catch (_: Exception) {
      startDeviceFlow()
    }
  }

  suspend fun refreshToken(token: BearerTokens): BearerTokens =
      client
          .submitForm(
              url =
                  buildUrl {
                        withOAuthBase()
                        appendPathSegments("oauth2", "token")
                      }
                      .toString(),
              formParameters =
                  parameters {
                    withClientId()
                    append("grant_type", "refresh_token")
                    append("refresh_token", token.refreshToken!!)
                  },
          )
          .body<TokenResponse>()
          .let { BearerTokens(it.accessToken, it.refreshToken) }

  suspend fun startDeviceFlow(): BearerTokens {
    val codeResponse = fetchDeviceCode()

    println(codeResponse.verificationUriComplete)

    return pollDeviceToken(
            codeResponse.deviceCode,
            100.seconds,
            codeResponse.interval.seconds,
        )
        .let { BearerTokens(it.accessToken, it.refreshToken) }
  }

  suspend fun fetchDeviceCode(): DeviceCodeResponse =
      client
          .submitForm(
              url =
                  buildUrl {
                        withOAuthBase()
                        appendPathSegments("oauth2", "device", "auth")
                      }
                      .toString(),
              formParameters =
                  parameters {
                    withClientId()
                    append("scope", "openid offline auth:server")
                  },
          )
          .body()

  suspend fun pollDeviceToken(
      code: String,
      timeout: Duration,
      interval: Duration,
  ): TokenResponse =
      withTimeout(timeout) {
        while (true) {
          runCatching {
                client
                    .submitForm(
                        url =
                            buildUrl {
                                  withOAuthBase()
                                  appendPathSegments("oauth2", "token")
                                }
                                .toString(),
                        formParameters =
                            parameters {
                              withClientId()
                              append("device_code", code)
                              append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                            },
                    )
                    .body<TokenResponse>()
              }
              .onSuccess {
                return@withTimeout it
              }

          delay(interval)
        }

        // Silly compiler
        @Suppress("UNREACHABLE_CODE") throw IllegalStateException()
      }

  fun ParametersBuilder.withClientId() {
    append("client_id", "hytale-server")
  }

  fun URLBuilder.withOAuthBase() {
    protocol = URLProtocol.HTTPS
    host = "oauth.accounts.hytale.com"
  }

  fun URLBuilder.withSessionBase() {
    protocol = URLProtocol.HTTPS
    host = "sessions.hytale.com"
  }

  fun URLBuilder.withAccountBase() {
    protocol = URLProtocol.HTTPS
    host = "account-data.hytale.com"
  }

  suspend fun getAssetBundleSuspend(patchline: String, version: String) =
      client
          .get {
            url {
              withAccountBase()
              appendPathSegments(
                  "game-assets",
                  "builds",
                  patchline,
                  "$version.zip",
              )
            }
          }
          .body<AssetBundleResponse>()
          .url

  override fun getAssetBundle(patchline: String, version: String) = runBlocking {
    getAssetBundleSuspend(patchline, version)
  }

  suspend fun getAvailableProfilesSuspend(): GetProfileResponse =
      client
          .get {
            url {
              withAccountBase()
              appendPathSegments("my-account", "get-profiles")
            }
          }
          .body()

  override fun getAvailableProfiles() {
    runBlocking { getAvailableProfilesSuspend() }
  }

  override fun createGameSession(uuid: String) {
    TODO("Not yet implemented")
  }

  override fun terminateGameSession(token: String) {
    TODO("Not yet implemented")
  }
}
