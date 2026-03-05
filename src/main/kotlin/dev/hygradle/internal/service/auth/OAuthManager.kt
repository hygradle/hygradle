package dev.hygradle.internal.service.auth

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import okhttp3.MediaType.Companion.toMediaType
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

abstract class OAuthManager : BuildService<BuildServiceParameters.None> {
  protected val service =
      Retrofit.Builder()
          .baseUrl(OAuthService.BASE_URL)
          .addConverterFactory(
              Json.asConverterFactory("application/json; charset=utf-8".toMediaType())
          )
          .build()
          .create<OAuthService>()

  private val accessMutex = Mutex()

  private lateinit var currentAccessToken: Token
  private lateinit var currentRefreshToken: Token

  fun getAccessToken() = runBlocking { getAccessTokenSuspend() }

  @OptIn(ExperimentalTime::class)
  suspend fun getAccessTokenSuspend(): Token {
    val codeResp = service.getDeviceCode()
    println(codeResp.verificationUriComplete)
    val tokenResp = pollForToken(codeResp.deviceCode, 30.seconds, codeResp.interval.seconds)

    print(tokenResp)
    return Token("", Clock.System.now() + tokenResp.expiresIn.seconds)
    //    when {
    //      !this::currentAccessToken.isInitialized -> {
    //        // Load token
    //      }
    //
    //      currentAccessToken.expired && !currentRefreshToken.expired -> {
    //        val resp = accessMutex.withLock { refreshAccessToken(currentRefreshToken) }
    //      }
    //
    //      currentAccessToken.expired && currentRefreshToken.expired -> {
    //        val resp = accessMutex.withLock { startDeviceAuth() }
    //      }
    //    }
    //
    //    return currentAccessToken
  }

  protected suspend fun pollForToken(
      code: String,
      timeout: Duration,
      interval: Duration,
  ) =
      withTimeout(timeout) {
        while (true) {
          println("Checking token...")
          runCatching { service.checkDeviceCode(code) }
              .onSuccess {
                return@withTimeout it
              }

          delay(interval)
        }

        // Silly compiler
        @Suppress("UNREACHABLE_CODE") throw IllegalStateException()
      }

  interface OAuthService {
    @FormUrlEncoded
    @POST("oauth2/device/auth")
    suspend fun getDeviceCode(
        @Field("client_id") clientId: String = CLIENT_ID,
        @Field("scope") scope: String = "openid offline auth:server",
    ): GetDeviceCodeResponse

    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun checkDeviceCode(
        @Field("device_code") deviceCode: String,
        @Field("client_id") clientId: String = CLIENT_ID,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code",
    ): AccessTokenResponse

    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun exchangeRefreshToken(
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String = CLIENT_ID,
        @Field("grant_type") grantType: String = "refresh_token",
    ): AccessTokenResponse

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JsonIgnoreUnknownKeys
    data class GetDeviceCodeResponse(
        @SerialName("device_code") val deviceCode: String,
        @SerialName("user_code") val userCode: String,
        @SerialName("verification_uri") val verificationUri: String,
        @SerialName("verification_uri_complete") val verificationUriComplete: String,
        @SerialName("expires_in") val expiresIn: Int,
        val interval: Int,
    )

    @Serializable
    data class AccessTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("token_type") val tokenType: String,
        @SerialName("expires_in") val expiresIn: Int,
        @SerialName("refresh_token") val refreshToken: String,
        @SerialName("id_token") val idToken: String,
        val scope: String,
    )

    companion object {
      const val BASE_URL = "https://oauth.accounts.hytale.com"
      const val CLIENT_ID = "hytale-server"
    }
  }
}
