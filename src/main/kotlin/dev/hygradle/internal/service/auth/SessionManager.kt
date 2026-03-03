package dev.hygradle.internal.service.auth

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class SessionManager : BuildService<BuildServiceParameters.None> {}
