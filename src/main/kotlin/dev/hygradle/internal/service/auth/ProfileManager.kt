package dev.hygradle.internal.service.auth

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference

abstract class ProfileManager : BuildService<BuildServiceParameters.None> {
  @get:ServiceReference abstract val accessManager: Property<AccessManager>
}
