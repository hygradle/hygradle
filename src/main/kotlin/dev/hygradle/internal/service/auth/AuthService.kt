package dev.hygradle.internal.service.auth

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class AuthService : BuildService<AuthService.Parameters> {
  interface Parameters : BuildServiceParameters

  protected val authTokenMutex = Mutex()

  protected lateinit var token: AuthToken

  fun getAuthToken(): AuthToken = runBlocking { getAuthTokenSuspend() }

  suspend fun getAuthTokenSuspend(): AuthToken =
      // TODO: This lock probably doesn't need to be on every call, lock only on load/write?
      authTokenMutex.withLock {
        if (!this::token.isInitialized)
            try {
              this.token = loadExistingToken()
            } catch (_: Exception) {
              this.token = OAuth.tokenFromBrowserFlow()
            }

        if (isTokenExpired(token)) this.token = OAuth.tokenFromBrowserFlow()

        token
      }

  protected fun loadExistingToken(): AuthToken {
    throw Exception("teehee")
  }

  @OptIn(ExperimentalTime::class)
  protected fun isTokenExpired(token: AuthToken): Boolean =
      Clock.System.now().toEpochMilliseconds() > token.expiry
}

@Serializable data class AuthToken(val token: String, val expiry: Long)
