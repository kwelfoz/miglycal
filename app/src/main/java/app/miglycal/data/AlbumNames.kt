package app.miglycal.data

import java.text.Normalizer
import java.util.Locale

/** Treat common release-edition suffixes as metadata, not a separate album identity. */
object AlbumNames {
    private val editionSuffix = Regex(
        """(?i)\s*(?:[-–—:]\s*|\s*\(\s*|\s*\[\s*|\s+)(?:super\s+)?(?:deluxe|expanded|special|anniversary|remaster(?:ed)?|bonus(?:\s+tracks?)?|collector'?s|limited(?:\s+edition)?|extended(?:\s+version)?|complete(?:\s+edition)?|standard(?:\s+edition)?|explicit(?:\s+version)?|clean(?:\s+version)?|edition)\b.*$"""
    )

    fun baseTitle(title: String): String {
        var result = title.trim()
        repeat(4) {
            val next = editionSuffix.replace(result, "").trim().trimEnd('-', '–', '—', ':', '(', '[', ' ')
            if (next == result) return result
            result = next
        }
        return result
    }

    private fun normalize(title: String): String =
        Normalizer.normalize(baseTitle(title).lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    fun sameRelease(first: String, second: String): Boolean =
        normalize(first).isNotBlank() && normalize(first) == normalize(second)
}
