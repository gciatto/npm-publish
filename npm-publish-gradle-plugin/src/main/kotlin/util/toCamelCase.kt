package dev.petuska.npm.publish.util

import java.util.Locale
import java.util.regex.Pattern

private val WORD_SEPARATOR = Pattern.compile("\\W+")

internal fun String.toCamelCase(lower: Boolean = false): String {
  val builder = StringBuilder()
  val matcher = WORD_SEPARATOR.matcher(this)
  var pos = 0
  var first = true
  while (matcher.find()) {
    var chunk: String = subSequence(pos, matcher.start()).toString()
    pos = matcher.end()
    if (chunk.isEmpty()) {
      continue
    }
    chunk = chunk.replaceFirstChar {
      when {
        first && lower -> it.lowercase(Locale.getDefault()).also { first = false }
        it.isLowerCase() -> it.titlecase(Locale.getDefault())
        else -> it.toString()
      }
    }
    builder.append(chunk)
  }
  var rest: String = subSequence(pos, length).toString()
  rest = rest.replaceFirstChar {
    when {
      first && lower -> it.lowercase(Locale.getDefault()).also { first = false }
      it.isLowerCase() -> it.titlecase(Locale.getDefault())
      else -> it.toString()
    }
  }
  builder.append(rest)
  return builder.toString()
}
