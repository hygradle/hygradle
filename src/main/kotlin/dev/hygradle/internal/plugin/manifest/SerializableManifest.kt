package dev.hygradle.internal.plugin.manifest

import dev.hygradle.dsl.plugin.manifest.Author
import dev.hygradle.dsl.plugin.manifest.Dependency
import dev.hygradle.dsl.plugin.manifest.Manifest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SerializableManifest(
    @SerialName("Name") val name: String,
    @SerialName("Group") val group: String,
    @SerialName("Version") val version: String,
    @SerialName("Description") val description: String? = null,
    @SerialName("Website") val website: String? = null,
    @SerialName("ServerVersion") val serverVersion: String,
    @SerialName("Main") val mainClass: String,
    @SerialName("IncludesAssetPack") val includesAssetPack: Boolean,
    @SerialName("Authors") val authors: List<SerializableAuthor>? = null,
    @SerialName("Dependencies") val dependencies: Map<String, String>? = null,
    @SerialName("OptionalDependencies") val optionalDependencies: Map<String, String>? = null,
)

@Serializable
data class SerializableAuthor(
    @SerialName("Name") val name: String,
    @SerialName("Email") val email: String? = null,
    @SerialName("Url") val url: String? = null,
)

fun Manifest.toSerializable(): SerializableManifest {
  val (optional, required) = dependencies.partition { it.optional.get() }

  return SerializableManifest(
      name = name.get(),
      group = group.get(),
      version = version.get(),
      description = description.orNull,
      website = website.orNull,
      serverVersion = serverVersion.get(),
      mainClass = mainClass.get(),
      includesAssetPack = includesAssetPack.get(),
      authors = authors.map { it.toSerializable() }.ifEmpty { null },
      dependencies = required.associate { it.toKey() to it.version.get() }.ifEmpty { null },
      optionalDependencies = optional.associate { it.toKey() to it.version.get() }.ifEmpty { null },
  )
}

fun Author.toSerializable(): SerializableAuthor =
    SerializableAuthor(
        name = name.get(),
        email = email.orNull,
        url = url.orNull,
    )

private fun Dependency.toKey(): String = "${group.get()}:${name.get()}"
