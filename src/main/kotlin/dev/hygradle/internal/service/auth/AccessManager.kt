package dev.hygradle.internal.service.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.gradle.api.GradleException
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class AccessManager : BuildService<BuildServiceParameters.None> {
  protected val httpClient = HttpClient(CIO) { install(ContentNegotiation) { json() } }

  fun getAccessToken(): String {
    TODO("Not yet implemented")
  }

  suspend fun getAccessTokenSuspend(): String {
    startDeviceAuth()
    return ""
  }

  protected suspend fun startDeviceAuth() {
    val response = requestDeviceCode()
    println("PLEASE GO TO THIS URL AND LOG IN ${response.verificationUriComplete}")
    try {
      val token =
          pollForToken(
              response.deviceCode,
              response.expiresIn.seconds,
              response.interval.seconds,
          )
              as AccessTokenResponse
    } catch (e: TimeoutCancellationException) {
      throw GradleException("Authorization timed out.")
    } catch (e: Exception) {
      throw e
    }
  }

  protected suspend fun requestDeviceCode(): RequestDeviceCodeResponse =
      httpClient
          .submitForm(
              "https://oauth.accounts.hytale.com/oauth2/device/auth",
              formParameters =
                  parameters {
                    append("client_id", "hytale-server")
                    append("scope", listOf("openid", "offline", "auth:server").joinToString(" "))
                  },
          )
          .body()

  protected suspend fun pollForToken(
      code: String,
      timeout: Duration,
      interval: Duration,
  ): Any =
      withTimeout(timeout) {
        println("Polling for token...")

        while (true) {
          val resp =
              httpClient.submitForm(
                  "https://oauth.accounts.hytale.com/oauth2/token",
                  formParameters =
                      parameters {
                        append("client_id", "hytale-server")
                        append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                        append("device_code", code)
                      },
              )

          if (resp.status.isSuccess()) {
            return@withTimeout resp.body<AccessTokenResponse>()
          }

          delay(interval)
        }
      }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class RequestDeviceCodeResponse(
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
    val scope: String,
)
