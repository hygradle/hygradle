package dev.hygradle.functionaltest

enum GradleDsl {
	GROOVY("settings.gradle", "build.gradle"),
	KOTLIN("settings.gradle.kts", "build.gradle.kts")

	final String settingsFileName
	final String buildFileName

	GradleDsl(String settingsFileName, String buildFileName) {
		this.settingsFileName = settingsFileName
		this.buildFileName = buildFileName
	}
}
