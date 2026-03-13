package dev.hygradle.internal.service.hytale

import dev.hygradle.dsl.hytale.Patchline
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.gradle.api.logging.Logger

class HytaleServiceImpl(
    engine: HttpClientEngine,
    private val oauthBaseUrl: String,
    private val accountBaseUrl: String,
    private val sessionBaseUrl: String,
    private val logger: Logger,
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

    logger.lifecycle(
        """
      Starting OAuth device code flow...
      ===================================================================
      Please open this URL in your browser to authenticate: ${codeResponse.verificationUriComplete}
      
      Alternatively, go to ${codeResponse.verificationUri} and enter the code '${codeResponse.userCode}'.
      ===================================================================
    """
            .trimIndent()
    )

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
    takeFrom(oauthBaseUrl)
  }

  fun URLBuilder.withSessionBase() {
    takeFrom(sessionBaseUrl)
  }

  fun URLBuilder.withAccountBase() {
    takeFrom(accountBaseUrl)
  }

  suspend fun getAssetBundleSuspend(patchline: Patchline, version: String) =
      client
          .get {
            url {
              withAccountBase()
              appendPathSegments(
                  "game-assets",
                  "builds",
                  patchline.cdnSlug,
                  "$version.zip",
              )
            }
          }
          .body<AssetBundleResponse>()
          .url

  override fun getAssetBundle(patchline: Patchline, version: String) = runBlocking {
    getAssetBundleSuspend(patchline, version)
  }

  suspend fun getAvailableProfilesSuspend(): List<Profile> =
      client
          .get {
            url {
              withAccountBase()
              appendPathSegments("my-account", "get-profiles")
            }
          }
          .body<GetProfileResponse>()
          .profiles

  override fun getAvailableProfiles(): List<Profile> = runBlocking { getAvailableProfilesSuspend() }

  suspend fun createGameSessionSuspend(uuid: String) =
      client
          .post {
            url {
              withSessionBase()
              appendPathSegments("game-session", "new")
            }

            contentType(ContentType.Application.Json)
            setBody(CreateGameSessionRequest(uuid))
          }
          .body<CreateGameSessionResponse>()
          .let { SessionTokens(it.sessionToken, it.identityToken) }

  override fun createGameSession(uuid: String) = runBlocking { createGameSessionSuspend(uuid) }

  override fun terminateGameSession(token: String) {
    TODO("Not yet implemented")
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

@Serializable data class SessionTokens(val sessionToken: String, val identityToken: String)
