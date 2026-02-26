package dev.hygradle.internal.service

import io.ktor.http.*
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

class OAuth {
  companion object {
    suspend fun awaitAuthCode(csrfState: String) =
        withTimeout(20.seconds) {
          val authCode = CompletableDeferred<String>()

          val server =
              embeddedServer(ServerCIO, host = "127.0.0.1", port = 80) {
                routing {
                  get("/authorization-callback") {
                    val code = call.parameters["code"]
                    val state = call.parameters["state"]

                    when {
                      state.isNullOrEmpty() || csrfState != state -> {
                        call.respond(HttpStatusCode.BadRequest, "Invalid state")
                        authCode.completeExceptionally(Exception("fuck"))
                      }

                      code.isNullOrEmpty() -> {
                        call.respond(HttpStatusCode.BadRequest, "No code received")
                        authCode.completeExceptionally(Exception("fuck"))
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
  }
}
