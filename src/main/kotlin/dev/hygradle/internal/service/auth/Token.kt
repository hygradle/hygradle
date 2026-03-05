package dev.hygradle.internal.service.auth

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class Token(val raw: String, val expiry: Instant) {
  @OptIn(ExperimentalTime::class)
  val expired
    get() = Clock.System.now() > expiry
}
