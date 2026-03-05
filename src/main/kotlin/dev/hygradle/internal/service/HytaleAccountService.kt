package dev.hygradle.internal.service

import dev.hygradle.dsl.hytale.Patchline
import dev.hygradle.internal.service.auth.OAuthManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class HytaleAccountService : BuildService<BuildServiceParameters.None> {
  protected val client =
      HttpClient(CIO) {
        install(ContentNegotiation) { json() }

        install(Auth) {
          bearer {
            sendWithoutRequest { req ->
              listOf("account-data.hytale.com", "sessions.hytale.com").contains(req.url.host)
            }

            refreshTokens { refreshOrRenewToken(client, oldTokens!!) }
          }
        }
      }

  suspend fun refreshOrRenewToken(httpClient: HttpClient, oldTokens: BearerTokens): BearerTokens {
    val newTokens: OAuthManager.OAuthService.AccessTokenResponse =
        try {
          httpClient
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
                        append("refresh_token", oldTokens.refreshToken!!)
                      },
              )
              .body()
        } catch (e: ClientRequestException) {
          when {
            e.response.status == HttpStatusCode.Unauthorized -> {
              // Make user log in again
              val codeResponse = fetchDeviceCode()

              println(codeResponse.verificationUriComplete)

              val tokens =
                  pollDeviceToken(
                      codeResponse.deviceCode,
                      30.seconds,
                      codeResponse.interval.seconds,
                  )

              return BearerTokens(tokens.accessToken, tokens.refreshToken)
            }
            else -> throw e
          }
        }

    return BearerTokens(newTokens.accessToken, newTokens.refreshToken)
  }

  protected suspend fun fetchDeviceCode(): OAuthManager.OAuthService.GetDeviceCodeResponse =
      client
          .get {
            url {
              withOAuthBase()
              appendPathSegments("oauth2", "device", "auth")
            }
            parameters {
              withClientId()
              append("scope", "openid offline auth:server")
            }
          }
          .body()

  protected suspend fun pollDeviceToken(
      code: String,
      timeout: Duration,
      interval: Duration,
  ) =
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
                            },
                    )
                    .body<OAuthManager.OAuthService.AccessTokenResponse>()
              }
              .onSuccess {
                return@withTimeout it
              }

          delay(interval)
        }

        // Silly compiler
        @Suppress("UNREACHABLE_CODE") throw IllegalStateException()
      }

  protected fun ParametersBuilder.withClientId() {
    append("client_id", "hytale-server")
  }

  protected fun URLBuilder.withOAuthBase() {
    host = "oauth.accounts.hytale.com"
  }

  protected fun URLBuilder.withSessionBase() {
    host = "sessions.hytale.com"
  }

  protected fun URLBuilder.withAccountBase() {
    host = "account-data.hytale.com"
  }

  fun getAvailableProfiles() = runBlocking { getAvailableProfilesSuspend() }

  fun getAssetBundle(version: String, patchline: Patchline) = runBlocking {
    getAssetBundleSuspend(version, patchline)
  }

  suspend fun getAvailableProfilesSuspend() =
      client.get {
        url {
          withAccountBase()
          appendPathSegments("my-account", "get-profiles")
        }
      }

  suspend fun getAssetBundleSuspend(version: String, patchline: Patchline) =
      client.get {
        url {
          withAccountBase()
          appendPathSegments(
              "game-assets",
              "builds",
              patchline.toString().lowercase(),
              version,
              ".zip",
          )
        }
      }
}
