package app.steptwelve.domain

/**
 * Whole-token, case-insensitive tag matching.
 *
 * Text is split into tokens on any character that is not a letter or digit, so
 * "John (AA)" yields [john, aa]. A tag matches when one or more *consecutive*
 * tokens, joined together, equal the tag with its own separators removed. That
 * makes `12step` match "12 step", "12-step" and "12step", while `aa` never
 * matches inside "Aaron".
 */
object TagMatcher {

    private val separators = Regex("[^\\p{L}\\p{N}]+")

    fun tokenize(text: String): List<String> =
        text.lowercase().split(separators).filter { it.isNotEmpty() }

    /** The canonical form of a tag: lowercase with separators removed ("12-Step" -> "12step"). */
    fun normalizeTag(tag: String): String = tokenize(tag).joinToString("")

    fun matches(text: String, tag: String): Boolean = matches(tokenize(text), normalizeTag(tag))

    /** Returns the tags (as given) that match [text]. */
    fun matchingTags(text: String, tags: Collection<String>): List<String> {
        val tokens = tokenize(text)
        return tags.filter { matches(tokens, normalizeTag(it)) }
    }

    private fun matches(tokens: List<String>, normalizedTag: String): Boolean {
        if (normalizedTag.isEmpty()) return false
        for (start in tokens.indices) {
            val joined = StringBuilder()
            for (end in start until tokens.size) {
                joined.append(tokens[end])
                if (joined.length > normalizedTag.length) break
                if (joined.length == normalizedTag.length) {
                    if (joined.toString() == normalizedTag) return true
                    break
                }
            }
        }
        return false
    }
}
