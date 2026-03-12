package dev.hygradle.test.internal.service.hytale

import dev.hygradle.dsl.hytale.Patchline
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.content.*
import java.util.concurrent.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HytaleServiceImplTest {

  @Test
  fun `getAssetBundle constructs correct URL with patchline slug and version`() {
    val mock =
        MockServiceBuilder().enqueue("""{"url":"https://cdn.hytale.com/bundle.zip"}""").build()

    val result = mock.service.getAssetBundle(Patchline.RELEASE, "1.0.0")

    assertEquals("https://cdn.hytale.com/bundle.zip", result)
    assertEquals(1, mock.requests.size)
    assertEquals("account.test.local", mock.requests[0].url.host)
    assertEquals("/game-assets/builds/release/1.0.0.zip", mock.requests[0].url.encodedPath)
  }

  @Test
  fun `getAssetBundle uses patchline cdnSlug for pre-release`() {
    val mock =
        MockServiceBuilder().enqueue("""{"url":"https://cdn.hytale.com/pre-release.zip"}""").build()

    mock.service.getAssetBundle(Patchline.PRERELEASE, "2.0.0")

    assertEquals(
        "/game-assets/builds/pre-release/2.0.0.zip",
        mock.requests[0].url.encodedPath,
    )
  }

  @Test
  fun `getAssetBundle authenticates via 401 challenge and retries with bearer token`() {
    val mock =
        MockServiceBuilder()
            .withTokens("loaded-access", "loaded-refresh")
            .enqueue("{}", 401)
            .enqueue("""{"access_token":"refreshed-access","refresh_token":"refreshed-refresh"}""")
            .enqueue("""{"url":"https://cdn.hytale.com/authed.zip"}""")
            .build()

    val result = mock.service.getAssetBundle(Patchline.RELEASE, "1.0.0")

    assertEquals("https://cdn.hytale.com/authed.zip", result)
    assertEquals(3, mock.requests.size)
    assertEquals("Bearer refreshed-access", mock.requests[2].headers["Authorization"])
  }

  @Test
  fun `getAvailableProfiles constructs correct URL path`() {
    val mock =
        MockServiceBuilder()
            .enqueue(
                """{"owner":"test-owner","profiles":[{"uuid":"abc-123","username":"player1"}]}"""
            )
            .build()

    mock.service.getAvailableProfiles()

    assertEquals(1, mock.requests.size)
    assertEquals("account.test.local", mock.requests[0].url.host)
    assertEquals("/my-account/get-profiles", mock.requests[0].url.encodedPath)
  }

  @Test
  fun `refreshToken sends correct form parameters and returns new tokens`() = runBlocking {
    val mock =
        MockServiceBuilder()
            .enqueue("""{"access_token":"new-access","refresh_token":"new-refresh"}""")
            .build()

    val result = mock.service.refreshToken(BearerTokens("old-access", "old-refresh"))

    assertEquals("new-access", result.accessToken)
    assertEquals("new-refresh", result.refreshToken)
    assertEquals("oauth.test.local", mock.requests[0].url.host)
    assertEquals("/oauth2/token", mock.requests[0].url.encodedPath)

    val params = mock.formParams(0)
    assertEquals("refresh_token", params["grant_type"])
    assertEquals("hytale-server", params["client_id"])
    assertEquals("old-refresh", params["refresh_token"])
  }

  @Test
  fun `fetchDeviceCode sends correct form parameters and deserializes response`() = runBlocking {
    val mock =
        MockServiceBuilder()
            .enqueue(
                """{"device_code":"dev-123","user_code":"ABCD-EFGH","verification_uri":"https://verify.example.com","verification_uri_complete":"https://verify.example.com?code=ABCD-EFGH","interval":5}"""
            )
            .build()

    val result = mock.service.fetchDeviceCode()

    assertEquals("dev-123", result.deviceCode)
    assertEquals("ABCD-EFGH", result.userCode)
    assertEquals("https://verify.example.com", result.verificationUri)
    assertEquals("https://verify.example.com?code=ABCD-EFGH", result.verificationUriComplete)
    assertEquals(5, result.interval)

    val params = mock.formParams(0)
    assertEquals("hytale-server", params["client_id"])
    assertEquals("openid offline auth:server", params["scope"])
  }

  @Test
  fun `pollDeviceToken retries on failure then succeeds`() = runBlocking {
    val mock =
        MockServiceBuilder()
            .enqueue("""{"error":"authorization_pending"}""", 400)
            .enqueue("""{"access_token":"polled-access","refresh_token":"polled-refresh"}""")
            .build()

    val result =
        mock.service.pollDeviceToken(
            "device-code-1",
            5.seconds,
            100.milliseconds,
        )

    assertEquals("polled-access", result.accessToken)
    assertEquals("polled-refresh", result.refreshToken)
    assertEquals(2, mock.requests.size)
  }

  @Test
  fun `pollDeviceToken times out when token endpoint never succeeds`() {
    val mock = MockServiceBuilder().build()

    assertThrows<CancellationException> {
      runBlocking {
        mock.service.pollDeviceToken(
            "device-code-1",
            300.milliseconds,
            50.milliseconds,
        )
      }
    }
  }

  @Test
  fun `createGameSession posts to correct URL with JSON body and returns session tokens`() {
    val mock =
        MockServiceBuilder()
            .enqueue(
                """{"sessionToken":"sess-tok-1","identityToken":"id-tok-1","expiresAt":"2026-01-01T00:00:00Z"}"""
            )
            .build()

    val result = mock.service.createGameSession("test-uuid")

    assertEquals(1, mock.requests.size)
    assertEquals("session.test.local", mock.requests[0].url.host)
    assertEquals("/game-session/new", mock.requests[0].url.encodedPath)
    assertEquals(
        "application/json",
        mock.requests[0].body.contentType?.toString()?.substringBefore(";"),
    )
    assertEquals("""{"uuid":"test-uuid"}""", (mock.requests[0].body as TextContent).text)
    assertEquals("sess-tok-1", result.sessionToken)
    assertEquals("id-tok-1", result.identityToken)
  }

  @Test
  fun `createGameSession authenticates via 401 challenge and retries with bearer token`() {
    val mock =
        MockServiceBuilder()
            .withTokens("loaded-access", "loaded-refresh")
            .enqueue("{}", 401)
            .enqueue("""{"access_token":"refreshed-access","refresh_token":"refreshed-refresh"}""")
            .enqueue(
                """{"sessionToken":"sess-tok-2","identityToken":"id-tok-2","expiresAt":"2026-01-01T00:00:00Z"}"""
            )
            .build()

    val result = mock.service.createGameSession("test-uuid")

    assertEquals("sess-tok-2", result.sessionToken)
    assertEquals("id-tok-2", result.identityToken)
    assertEquals(3, mock.requests.size)
    assertEquals("Bearer refreshed-access", mock.requests[2].headers["Authorization"])
  }
}
