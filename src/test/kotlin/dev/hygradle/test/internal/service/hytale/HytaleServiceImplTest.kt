package dev.hygradle.test.internal.service.hytale

import dev.hygradle.dsl.hytale.Patchline
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.content.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException

class HytaleServiceImplTest :
    FunSpec({
      context("getAssetBundle") {
        test("constructs the correct URL") {
          val mock =
              MockServiceBuilder()
                  .enqueue("""{"url":"https://cdn.hytale.com/bundle.zip"}""")
                  .build()

          val result = mock.service.getAssetBundle(Patchline.RELEASE, "1.0.0")

          result shouldBe "https://cdn.hytale.com/bundle.zip"
          mock.requests.size shouldBe 1
          mock.requests[0].url.host shouldBe "account.test.local"
          mock.requests[0].url.encodedPath shouldBe "/game-assets/builds/release/1.0.0.zip"
        }

        test("uses patchline cdnSlug for pre-release") {
          val mock =
              MockServiceBuilder()
                  .enqueue("""{"url":"https://cdn.hytale.com/pre-release.zip"}""")
                  .build()

          mock.service.getAssetBundle(Patchline.PRERELEASE, "2.0.0")

          mock.requests[0].url.encodedPath shouldBe "/game-assets/builds/pre-release/2.0.0.zip"
        }

        test("authenticates via 401 challenge and retries with bearer token") {
          val mock =
              MockServiceBuilder()
                  .withTokens("loaded-access", "loaded-refresh")
                  .enqueue("{}", 401)
                  .enqueue(
                      """{"access_token":"refreshed-access","refresh_token":"refreshed-refresh"}"""
                  )
                  .enqueue("""{"url":"https://cdn.hytale.com/authed.zip"}""")
                  .build()

          val result = mock.service.getAssetBundle(Patchline.RELEASE, "1.0.0")

          result shouldBe "https://cdn.hytale.com/authed.zip"
          mock.requests.size shouldBe 3
          mock.requests[2].headers["Authorization"] shouldBe "Bearer refreshed-access"
        }
      }

      context("getAvailableProfiles") {
        test("constructs correct URL path") {
          val mock =
              MockServiceBuilder()
                  .enqueue(
                      """{"owner":"test-owner","profiles":[{"uuid":"abc-123","username":"player1"}]}"""
                  )
                  .build()

          mock.service.getAvailableProfiles()

          mock.requests.size shouldBe 1
          mock.requests[0].url.host shouldBe "account.test.local"
          mock.requests[0].url.encodedPath shouldBe "/my-account/get-profiles"
        }
      }

      context("refreshToken") {
        test("sends correct form parameters and returns new tokens") {
          val mock =
              MockServiceBuilder()
                  .enqueue("""{"access_token":"new-access","refresh_token":"new-refresh"}""")
                  .build()

          val result = mock.service.refreshToken(BearerTokens("old-access", "old-refresh"))

          result.accessToken shouldBe "new-access"
          result.refreshToken shouldBe "new-refresh"
          mock.requests[0].url.host shouldBe "oauth.test.local"
          mock.requests[0].url.encodedPath shouldBe "/oauth2/token"

          val params = mock.formParams(0)

          params["grant_type"] shouldBe "refresh_token"
          params["client_id"] shouldBe "hytale-server"
          params["refresh_token"] shouldBe "old-refresh"
        }
      }

      context("fetchDeviceCode") {
        test("sends correct form parameters and deserializes response") {
          val mock =
              MockServiceBuilder()
                  .enqueue(
                      """{"device_code":"dev-123","user_code":"ABCD-EFGH","verification_uri":"https://verify.example.com","verification_uri_complete":"https://verify.example.com?code=ABCD-EFGH","interval":5}"""
                  )
                  .build()

          val result = mock.service.fetchDeviceCode()

          result.deviceCode shouldBe "dev-123"
          result.userCode shouldBe "ABCD-EFGH"
          result.verificationUri shouldBe "https://verify.example.com"
          result.verificationUriComplete shouldBe "https://verify.example.com?code=ABCD-EFGH"
          result.interval shouldBe 5

          val params = mock.formParams(0)

          params["client_id"] shouldBe "hytale-server"
          params["scope"] shouldBe "openid offline auth:server"
        }
      }

      context("pollDeviceToken") {
        test("retries on failure then succeeds") {
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

          result.accessToken shouldBe "polled-access"
          result.refreshToken shouldBe "polled-refresh"
          mock.requests.size shouldBe 2
        }

        test("times out when token endpoint never succeeds") {
          val mock = MockServiceBuilder().build()

          shouldThrow<CancellationException> {
            mock.service.pollDeviceToken(
                "device-code-1",
                300.milliseconds,
                50.milliseconds,
            )
          }
        }
      }

      context("createGameSession") {
        test("posts to correct URL with JSON body and returns session tokens") {
          val mock =
              MockServiceBuilder()
                  .enqueue(
                      """{"sessionToken":"sess-tok-1","identityToken":"id-tok-1","expiresAt":"2026-01-01T00:00:00Z"}"""
                  )
                  .build()

          val result = mock.service.createGameSession("test-uuid")

          mock.requests.size shouldBe 1
          mock.requests[0].url.host shouldBe "session.test.local"
          mock.requests[0].url.encodedPath shouldBe "/game-session/new"
          mock.requests[0].body.contentType?.toString()?.substringBefore(";") shouldBe
              "application/json"
          (mock.requests[0].body as TextContent).text shouldBe """{"uuid":"test-uuid"}"""
          result.sessionToken shouldBe "sess-tok-1"
          result.identityToken shouldBe "id-tok-1"
        }

        test("authenticates via 401 challenge and retries with bearer token") {
          val mock =
              MockServiceBuilder()
                  .withTokens("loaded-access", "loaded-refresh")
                  .enqueue("{}", 401)
                  .enqueue(
                      """{"access_token":"refreshed-access","refresh_token":"refreshed-refresh"}"""
                  )
                  .enqueue(
                      """{"sessionToken":"sess-tok-2","identityToken":"id-tok-2","expiresAt":"2026-01-01T00:00:00Z"}"""
                  )
                  .build()

          val result = mock.service.createGameSession("test-uuid")

          result.sessionToken shouldBe "sess-tok-2"
          result.identityToken shouldBe "id-tok-2"
          mock.requests.size shouldBe 3
          mock.requests[2].headers["Authorization"] shouldBe "Bearer refreshed-access"
        }
      }
    })
