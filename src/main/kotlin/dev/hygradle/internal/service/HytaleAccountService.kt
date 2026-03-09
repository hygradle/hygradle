package dev.hygradle.internal.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class HytaleAccountService : BuildService<HytaleAccountService.Parameters>, AutoCloseable {
  interface Parameters : BuildServiceParameters {
    val tokenFile: RegularFileProperty
  }

  protected val tokens = mutableListOf<BearerTokens>()

  @OptIn(ExperimentalSerializationApi::class)
  protected val client =
      HttpClient(CIO) {
        install(ContentNegotiation) { json() }

        install(Auth) {
          reAuthorizeOnResponse {
            it.status == HttpStatusCode.Unauthorized ||
                // TODO: Remove this when Hypixel sends real responses
                (it.request.url.host == "account-data.hytale.com" &&
                    it.status == HttpStatusCode.Forbidden)
          }

          bearer {
            sendWithoutRequest { true }

            loadTokens {
              try {
                val existingTokens: BearerTokens =
                    parameters.tokenFile
                        .get()
                        .asFile
                        .let { Json.decodeFromStream<SerializableBearerToken>(it.inputStream()) }
                        .let { BearerTokens(it.accessToken, it.refreshToken) }

                tokens.add(existingTokens)
                tokens.last()
              } catch (_: Exception) {
                null
              }
            }

            refreshTokens {
              val newTokens = refreshOrRenewToken(oldTokens)
              tokens.add(newTokens)
              tokens.last()
            }
          }
        }
      }

  override fun close() {
    parameters.tokenFile.get().asFile.also {
      it.parentFile.mkdirs()
      it.writeText(
          Json.encodeToString(
              SerializableBearerToken(tokens.last().accessToken, tokens.last().refreshToken!!)
          )
      )
    }
  }

  protected suspend fun refreshOrRenewToken(oldTokens: BearerTokens?): BearerTokens {
    if (oldTokens == null) return startDeviceFlow()

    return try {
      refreshToken(oldTokens)
    } catch (_: Exception) {
      startDeviceFlow()
    }
  }

  protected suspend fun refreshToken(token: BearerTokens): BearerTokens =
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

  protected suspend fun startDeviceFlow(): BearerTokens {
    val codeResponse = fetchDeviceCode()

    println(codeResponse.verificationUriComplete)

    return pollDeviceToken(
            codeResponse.deviceCode,
            100.seconds,
            codeResponse.interval.seconds,
        )
        .let { BearerTokens(it.accessToken, it.refreshToken) }
  }

  protected suspend fun fetchDeviceCode(): DeviceCodeResponse =
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

  protected suspend fun pollDeviceToken(
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

  protected fun ParametersBuilder.withClientId() {
    append("client_id", "hytale-server")
  }

  protected fun URLBuilder.withOAuthBase() {
    protocol = URLProtocol.HTTPS
    host = "oauth.accounts.hytale.com"
  }

  protected fun URLBuilder.withSessionBase() {
    protocol = URLProtocol.HTTPS
    host = "sessions.hytale.com"
  }

  protected fun URLBuilder.withAccountBase() {
    protocol = URLProtocol.HTTPS
    host = "account-data.hytale.com"
  }

  fun getAvailableProfiles() = runBlocking { getAvailableProfilesSuspend() }

  fun getAssetBundle(patchline: String, version: String) = runBlocking {
    getAssetBundleSuspend(patchline, version)
  }

  fun createGameSession(profileUuid: String) = runBlocking { createGameSessionSuspend(profileUuid) }

  suspend fun getAvailableProfilesSuspend(): GetProfileResponse =
      client
          .get {
            url {
              withAccountBase()
              appendPathSegments("my-account", "get-profiles")
            }
          }
          .body()

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

  suspend fun createGameSessionSuspend(profileUuid: String): CreateGameSessionResponse =
      client
          .post {
            url {
              withSessionBase()
              appendPathSegments("game-session", "new")
            }

            contentType(ContentType.Application.Json)
            setBody(CreateGameSessionRequest(profileUuid))
          }
          .body()

  fun terminateSession(sessionToken: String) = runBlocking { terminateSessionSuspend(sessionToken) }

  suspend fun terminateSessionSuspend(sessionToken: String) =
      // Use a separate client here because naturally auth is the session token itself
      HttpClient(CIO).delete {
        url {
          withSessionBase()
          appendPathSegments("game-session")
        }

        bearerAuth(sessionToken)
      }
}

@Serializable data class SerializableBearerToken(val accessToken: String, val refreshToken: String)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("verification_uri_complete") val verificationUriComplete: String,
    val interval: Int,
)

@Serializable data class AssetBundleResponse(val url: String)

@Serializable data class CreateGameSessionRequest(val uuid: String)

@Serializable
data class CreateGameSessionResponse(
    val sessionToken: String,
    val identityToken: String,
    val expiresAt: String,
)

@Serializable data class GetProfileResponse(val owner: String, val profiles: List<Profile>)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Profile(val uuid: String, val username: String)
