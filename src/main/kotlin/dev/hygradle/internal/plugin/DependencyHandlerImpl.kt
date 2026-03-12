package dev.hygradle.internal.plugin

import dev.hygradle.dsl.plugin.DependencyHandler
import javax.inject.Inject
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.DependencyScopeConfiguration

@Suppress("UnstableApiUsage")
abstract class DependencyHandlerImpl
@Inject
internal constructor(
    runtimeOnlyScope: NamedDomainObjectProvider<DependencyScopeConfiguration>,
    compileOnlyScope: NamedDomainObjectProvider<DependencyScopeConfiguration>,
) : DependencyHandler {
  init {
    runtimeOnlyScope.configure { fromDependencyCollector(runtimeOnly) }
    compileOnlyScope.configure { fromDependencyCollector(compileOnly) }
  }
}
