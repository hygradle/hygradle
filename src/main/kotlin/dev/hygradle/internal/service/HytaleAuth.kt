package dev.hygradle.internal.service

import io.ktor.client.*
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import java.awt.Desktop
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import org.gradle.api.GradleException
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class HytaleAuth : BuildService<BuildServiceParameters.None> {
  private val random = SecureRandom()

  private val httpClient = HttpClient(ClientCIO)

  suspend fun startBrowserFlow() {
    val csrfState = generateRandomString(32)
    val encodedState = stateWithPort(csrfState, 80)
    val codeVerifier = generateRandomString(64)
    val codeChallenge = generateCodeChallenge(codeVerifier)

    val authUri =
        buildAuthURI(encodedState, codeChallenge, "https://accounts.hytale.com/consent/client")

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
      val code = OAuth.awaitAuthCode(csrfState)
      println(code)
    } catch (_: TimeoutCancellationException) {
      throw GradleException("Authorization request timed out.")
    }

    //    println(exchangeForTokens(code, codeVerifier,
    // "https://accounts.hytale.com/consent/client"))
  }

  private fun stateWithPort(state: String, port: Int) =
      "{\"state\":\"${state}\",\"port\":\"${port}\"}"
          .let { Base64.UrlSafe.encode(it.toByteArray(StandardCharsets.US_ASCII)) }

  private fun buildAuthURI(state: String, codeChallenge: String, redirectUri: String) =
      URLBuilder(
              protocol = URLProtocol.HTTPS,
              host = "oauth.accounts.hytale.com",
              pathSegments = listOf("oauth2", "auth"),
              parameters =
                  ParametersBuilder()
                      .apply {
                        append("response_type", "code")
                        append("client_id", "hytale-server")
                        append("redirect_uri", redirectUri)
                        append("state", state)
                        append("code_challenge", codeChallenge)
                        append("code_challenge_method", "S256")
                        append(
                            "scopes",
                            listOf("openid", "offline", "auth:server").joinToString(" "),
                        )
                      }
                      .build(),
          )
          .buildString()

  fun generateRandomString(length: Int) =
      ByteArray(length).also { random.nextBytes(it) }.let { Base64.UrlSafe.encode(it) }

  private fun generateCodeChallenge(verifier: String) =
      MessageDigest.getInstance("SHA-256")
          .digest(verifier.toByteArray(StandardCharsets.US_ASCII))
          .let { Base64.UrlSafe.encode(it) }

  private suspend fun exchangeForTokens(
      code: String,
      verifier: String,
      redirectUri: String,
  ): HttpResponse {
    return httpClient.submitForm {
      url {
        protocol = URLProtocol.HTTPS
        host = "oauth.accounts.hytale.com"
        pathSegments = listOf("oauth2", "token")

        parameters {
          append("grant_type", "authorization_code")
          append("client_id", "hytale-server")
          append("code", code)
          append("redirect_uri", redirectUri)
          append("code_verifier", verifier)
        }
      }

      headers { append(HttpHeaders.UserAgent, "Hygradle") }
    }
  }
}
