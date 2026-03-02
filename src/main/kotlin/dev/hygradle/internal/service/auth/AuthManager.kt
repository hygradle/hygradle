package dev.hygradle.internal.service.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.awt.Desktop
import java.net.URI
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class AuthManager : BuildService<AuthManager.Parameters>, AutoCloseable {
  interface Parameters : BuildServiceParameters {
    val projectName: Property<String>
    val authFile: RegularFileProperty
  }

  protected val httpClient = HttpClient(ClientCIO) { install(ContentNegotiation) { json() } }

  protected val store = EncryptedStore(parameters.projectName, parameters.authFile)

  protected val accessTokenMutex = Mutex()

  protected lateinit var token: AccessToken

  fun getAccessToken(): AccessToken = runBlocking { getAccessTokenSuspend() }

  suspend fun getAccessTokenSuspend(): AccessToken =
      // TODO: This lock probably doesn't need to be on every call, lock only on load/write?
      accessTokenMutex.withLock {
        if (!this::token.isInitialized)
            try {
              this.token = store.load()
            } catch (_: Exception) {
              this.token = startOauthBrowserFlow()
            }

        if (isTokenExpired(token)) this.token = startOauthBrowserFlow()

        token
      }

  override fun close() = store.save(token)

  @OptIn(ExperimentalTime::class)
  suspend fun startOauthBrowserFlow(): AccessToken {
    val csrfState = generateRandomString(32)
    val encodedState =
        encodeBase64("{\"state\":\"${csrfState}\",\"port\":\"8080\"}".encodeToByteArray())
    val codeVerifier = generateRandomString(64)
    val codeChallenge = generateCodeChallenge(codeVerifier)

    val authUri = buildAuthURI(encodedState, codeChallenge)

    println("Starting OAuth browser flow...")
    println("===================================================================")
    println("Please open this URL in your browser to authenticate: $authUri")
    println("===================================================================")

    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      try {
        withContext(Dispatchers.IO) { Desktop.getDesktop().browse(URI(authUri)) }
        println("Browser opened automatically.")
      } catch (_: Exception) {
        println("Could not open browser automatically. Please open the URL manually.")
      }
    }

    try {
      val code = listenForAuthCode(csrfState)
      val tokenPayload = exchangeCodeForToken(code, codeVerifier)

      return AccessToken(
          tokenPayload.accessToken,
          Clock.System.now().plus(tokenPayload.expiresIn.seconds).toEpochMilliseconds(),
      )
    } catch (_: TimeoutCancellationException) {
      throw GradleException("Authorization request timed out.")
    }
  }

  protected suspend fun listenForAuthCode(csrfState: String) =
      // TODO: Make timeout configurable
      withTimeout(20.seconds) {
        val authCode = CompletableDeferred<String>()

        val server =
            // TODO: Make server port configurable
            embeddedServer(ServerCIO, host = "127.0.0.1", port = 8080) {
              routing {
                get("/authorization-callback") {
                  val code = call.parameters["code"]
                  val state = call.parameters["state"]

                  when {
                    state.isNullOrEmpty() || csrfState != state -> {
                      call.respond(HttpStatusCode.BadRequest, "Invalid state")
                      authCode.completeExceptionally(GradleException("Invalid state."))
                    }

                    code.isNullOrEmpty() -> {
                      call.respond(HttpStatusCode.BadRequest, "No code received")
                      authCode.completeExceptionally(
                          GradleException("No authorization code received.")
                      )
                    }

                    else -> {
                      call.respond(HttpStatusCode.OK)
                      authCode.complete(code)
                    }
                  }
                }
              }
            }

        server.start()

        try {
          authCode.await()
        } finally {
          server.stop()
        }
      }

  protected suspend fun exchangeCodeForToken(code: String, verifier: String): AccessTokenResponse =
      httpClient
          .submitForm(
              "https://oauth.accounts.hytale.com/oauth2/token",
              formParameters =
                  parameters {
                    append("grant_type", "authorization_code")
                    append("client_id", "hytale-server")
                    append("code", code)
                    append("redirect_uri", REDIRECT_URL)
                    append("code_verifier", verifier)
                  },
          )
          .body()

  protected fun buildAuthURI(state: String, codeChallenge: String) =
      URLBuilder(
              protocol = URLProtocol.HTTPS,
              host = "oauth.accounts.hytale.com",
              pathSegments = listOf("oauth2", "auth"),
              parameters =
                  parameters {
                    append("response_type", "code")
                    append("client_id", "hytale-server")
                    append("redirect_uri", REDIRECT_URL)
                    append("state", state)
                    append("code_challenge", codeChallenge)
                    append("code_challenge_method", "S256")
                    append(
                        "scopes",
                        listOf("openid", "offline", "auth:server").joinToString(" "),
                    )
                  },
          )
          .buildString()

  @OptIn(ExperimentalTime::class)
  protected fun isTokenExpired(token: AccessToken): Boolean =
      Clock.System.now().toEpochMilliseconds() > token.expiry

  protected fun generateRandomString(
      length: Int,
      charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_', '~'),
  ) = String(CharArray(length) { charPool.random() })

  protected fun generateCodeChallenge(verifier: String) =
      encodeBase64(
          MessageDigest.getInstance("SHA-256")
              .also { it.update(verifier.encodeToByteArray()) }
              .digest()
      )

  protected fun encodeBase64(bytes: ByteArray) =
      Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)

  companion object {
    const val REDIRECT_URL = "https://accounts.hytale.com/consent/client"
  }
}

@Serializable data class AccessToken(val token: String, val expiry: Long)

@Serializable
data class AccessTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
    val scope: String,
)
