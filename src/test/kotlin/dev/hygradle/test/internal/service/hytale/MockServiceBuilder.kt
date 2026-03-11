package dev.hygradle.test.internal.service.hytale

import dev.hygradle.internal.service.hytale.HytaleServiceImpl
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import java.util.concurrent.ConcurrentLinkedQueue
import org.slf4j.helpers.NOPLogger

class MockHytaleService(
    val service: HytaleServiceImpl,
    val engine: MockEngine,
) {
  val requests: List<HttpRequestData>
    get() = engine.requestHistory

  fun formParams(requestIndex: Int): Map<String, String> {
    val body = requests[requestIndex].body as FormDataContent
    return body.formData.entries().associate { (key, values) -> key to values.first() }
  }
}

class MockServiceBuilder {
  private data class QueuedResponse(val body: String, val status: HttpStatusCode)

  private val responses = ConcurrentLinkedQueue<QueuedResponse>()
  private var accessToken: String? = null
  private var refreshToken: String? = null

  fun enqueue(body: String, statusCode: Int = 200): MockServiceBuilder {
    responses.add(QueuedResponse(body, HttpStatusCode.fromValue(statusCode)))
    return this
  }

  fun withTokens(accessToken: String, refreshToken: String): MockServiceBuilder {
    this.accessToken = accessToken
    this.refreshToken = refreshToken
    return this
  }

  fun build(): MockHytaleService {
    val engine = MockEngine { _ ->
      val queued = responses.poll()
      if (queued != null) {
        respond(
            content = queued.body,
            status = queued.status,
            headers =
                headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString(),
                ),
        )
      } else {
        respondError(HttpStatusCode.InternalServerError)
      }
    }

    val at = accessToken
    val rt = refreshToken
    val tokenLoader: suspend () -> BearerTokens? = {
      if (at != null && rt != null) BearerTokens(at, rt) else null
    }

    val service =
        HytaleServiceImpl(
            engine = engine,
            oauthBaseUrl = "https://oauth.test.local",
            accountBaseUrl = "https://account.test.local",
            sessionBaseUrl = "https://session.test.local",
            logger = NOPLogger.NOP_LOGGER,
            tokenLoader = tokenLoader,
        )

    return MockHytaleService(service, engine)
  }
}
