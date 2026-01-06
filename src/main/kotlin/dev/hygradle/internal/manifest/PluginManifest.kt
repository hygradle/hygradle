package dev.hygradle.internal.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.provider.Property

public abstract class PluginManifest(pluginName: String) {
  public abstract val group: Property<String>
  public abstract val name: Property<String>
  public abstract val version: Property<String>
  public abstract val description: Property<String>

  init {
    name.convention(pluginName)
  }

  public fun serializable(): SerializablePluginManifest =
      SerializablePluginManifest(group.get(), name.get(), version.get())
}

@Serializable
public data class SerializablePluginManifest(
    @SerialName("Group") val group: String,
    @SerialName("Name") val name: String,
    @SerialName("Version") val version: String,
)
