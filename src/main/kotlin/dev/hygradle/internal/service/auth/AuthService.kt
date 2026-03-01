package dev.hygradle.internal.service.auth

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class AuthService : BuildService<AuthService.Parameters>, AutoCloseable {
  interface Parameters : BuildServiceParameters {
    val projectName: Property<String>
    val authFile: RegularFileProperty
  }

  val store = EncryptedStore(parameters.projectName, parameters.authFile)

  protected val authTokenMutex = Mutex()

  protected lateinit var token: AuthToken

  fun getAccessToken(): AuthToken = runBlocking { getAccessTokenSuspend() }

  suspend fun getAccessTokenSuspend(): AuthToken =
      // TODO: This lock probably doesn't need to be on every call, lock only on load/write?
      authTokenMutex.withLock {
        if (!this::token.isInitialized)
            try {
              this.token = store.load()
            } catch (_: Exception) {
              this.token = OAuth.tokenFromBrowserFlow()
            }

        if (isTokenExpired(token)) this.token = OAuth.tokenFromBrowserFlow()

        token
      }

  override fun close() = store.save(token)

  @OptIn(ExperimentalTime::class)
  protected fun isTokenExpired(token: AuthToken): Boolean =
      Clock.System.now().toEpochMilliseconds() > token.expiry
}

@Serializable data class AuthToken(val token: String, val expiry: Long)

@Serializable data class SessionTokens(val identity: String, val session: String)
