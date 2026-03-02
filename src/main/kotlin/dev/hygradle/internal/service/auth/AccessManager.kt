package dev.hygradle.internal.service.auth

interface AccessManager {
  fun getAccessToken(): String

  suspend fun getAccessTokenSuspend(): String
}
