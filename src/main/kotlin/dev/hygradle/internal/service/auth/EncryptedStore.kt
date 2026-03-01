package dev.hygradle.internal.service.auth

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider

/**
 * A naive encryption store for Hytale authentication tokens.
 *
 * Derives the encryption key similar to the internal
 * `com.hypixel.hytale.server.core.auth.EncryptedAuthCredentialStore`, using the Gradle project name
 * instead of the hardware UUID. This results in the same threat model as the Hytale server - if one
 * has access to the Gradle source one can derive the key, just as access to the Hytale server
 * hardware would allow one to derive that key.
 *
 * TODO: Is it worth using the hardware UUID?
 */
class EncryptedStore(
    val projectName: Provider<String>,
    val authFile: RegularFileProperty,
) {
  val encryptionKey: SecretKey = derive()

  fun load(): AuthToken =
      Json.decodeFromString(decrypt(authFile.get().asFile.readBytes()).decodeToString())

  fun save(token: AuthToken) {
    authFile.get().asFile.parentFile.mkdirs()
    authFile.get().asFile.writeBytes(encrypt(Json.encodeToString(token).encodeToByteArray()))
  }

  fun decrypt(encrypted: ByteArray): ByteArray {
    val buffer = ByteBuffer.wrap(encrypted)
    val iv = ByteArray(GCM_IV_LENGTH)
    buffer.get(iv)
    val cipherText = ByteArray(buffer.remaining())
    buffer.get(cipherText)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, encryptionKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

    return cipher.doFinal(cipherText)
  }

  fun encrypt(plain: ByteArray): ByteArray {
    val iv = ByteArray(GCM_IV_LENGTH)
    SecureRandom().nextBytes(iv)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    val cipherText = cipher.doFinal(plain)

    return iv + cipherText
  }

  fun derive(): SecretKey {
    val spec = PBEKeySpec(projectName.get().toCharArray(), SALT, PBKDF2_ITERATIONS, KEY_LENGTH)

    return SecretKeySpec(
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
        "AES",
    )
  }

  companion object {
    const val ALGORITHM = "AES/GCM/NoPadding"
    const val GCM_IV_LENGTH = 12
    const val GCM_TAG_LENGTH = 128
    const val KEY_LENGTH = 256
    const val PBKDF2_ITERATIONS = 100000

    val SALT = "Hygradle".toByteArray(StandardCharsets.UTF_8)
  }
}
