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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.GradleException

object OAuth {
  private const val REDIRECT_URL = "https://accounts.hytale.com/consent/client"
  private val httpClient = HttpClient(ClientCIO) { install(ContentNegotiation) { json() } }

  @OptIn(ExperimentalTime::class)
  suspend fun tokenFromBrowserFlow(): AuthToken {
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
      val code = awaitAuthCode(csrfState)
      val tokenPayload = fetchAccessToken(code, codeVerifier)

      return AuthToken(
          tokenPayload.accessToken,
          Clock.System.now().plus(tokenPayload.expiresIn.seconds).toEpochMilliseconds(),
      )
    } catch (_: TimeoutCancellationException) {
      throw GradleException("Authorization request timed out.")
    }
  }

  private suspend fun awaitAuthCode(csrfState: String) =
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

  private suspend fun fetchAccessToken(code: String, verifier: String): AuthTokenPayload =
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

  private fun buildAuthURI(state: String, codeChallenge: String) =
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

  private fun generateRandomString(
      length: Int,
      charPool: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_', '~'),
  ) = String(CharArray(length) { charPool.random() })

  private fun generateCodeChallenge(verifier: String) =
      encodeBase64(
          MessageDigest.getInstance("SHA-256")
              .also { it.update(verifier.encodeToByteArray()) }
              .digest()
      )

  private fun encodeBase64(bytes: ByteArray) =
      Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)
}

@Serializable
data class AuthTokenPayload(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
    val scope: String,
)
