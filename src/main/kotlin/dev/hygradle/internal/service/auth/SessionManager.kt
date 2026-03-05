package dev.hygradle.internal.service.auth

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

abstract class SessionManager : BuildService<BuildServiceParameters.None> {
  @get:ServiceReference abstract val oauthManager: Property<OAuthManager>

  protected val service =
      Retrofit.Builder()
          .baseUrl(SessionService.BASE_URL)
          .addConverterFactory(
              Json.asConverterFactory("application/json; charset=utf-8".toMediaType())
          )
          .build()
          .create<SessionService>()

  @OptIn(ExperimentalTime::class)
  suspend fun createSession(profileUUID: String): SessionTokens {
    val token = oauthManager.get().getAccessTokenSuspend()

    val response =
        service.createSession(
            profileUUID,
            "Bearer ${token.raw}",
        )

    return SessionTokens(
        Token(response.sessionToken, Instant.parse(response.expiresAt)),
        Token(response.identityToken, Instant.parse(response.expiresAt)),
    )
  }

  interface SessionService {
    @POST("game-session/new")
    suspend fun createSession(
        @Body uuid: String,
        @Header("Authorization") authorization: String,
    ): CreateGameSessionResponse

    @Serializable
    data class CreateGameSessionResponse(
        val sessionToken: String,
        val identityToken: String,
        val expiresAt: String,
    )

    companion object {
      const val BASE_URL = "https://sessions.hytale.com"
    }
  }
}

data class SessionTokens(val session: Token, val identity: Token)
