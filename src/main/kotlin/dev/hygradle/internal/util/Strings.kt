package dev.hygradle.internal.util

import java.util.Locale.getDefault

internal fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString()
}
