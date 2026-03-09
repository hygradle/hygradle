package dev.hygradle.internal.service.hytale

import dev.hygradle.internal.service.SerializableBearerToken
import io.ktor.client.plugins.auth.providers.*
import java.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class HytaleAccount : BuildService<HytaleAccount.Parameters> {
  interface Parameters : BuildServiceParameters {
    val tokenFile: RegularFileProperty
  }

  val service =
      ServiceLoader.load(HytaleServiceProvider::class.java).last().create(this::loadTokens)

  @OptIn(ExperimentalSerializationApi::class)
  private fun loadTokens(): BearerTokens =
      parameters.tokenFile
          .get()
          .asFile
          .let { Json.decodeFromStream<SerializableBearerToken>(it.inputStream()) }
          .let { BearerTokens(it.accessToken, it.refreshToken) }
}
