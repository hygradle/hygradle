package dev.hygradle.internal.service.auth

import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.gradle.api.model.ObjectFactory
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.newInstance

abstract class AuthService @Inject constructor(objects: ObjectFactory) :
    BuildService<AuthService.Parameters>, AutoCloseable {
  interface Parameters : BuildServiceParameters

  val store = objects.newInstance<EncryptedStore>()

  protected val authTokenMutex = Mutex()

  protected lateinit var token: AuthToken

  fun getAuthToken(): AuthToken = runBlocking { getAuthTokenSuspend() }

  suspend fun getAuthTokenSuspend(): AuthToken =
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
