package dev.hygradle.internal.service.hytale

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class HytaleAccount :
    BuildService<BuildServiceParameters.None>, HytaleService by HytaleServiceImpl()
