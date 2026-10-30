package app.miglycal.data

import java.text.Normalizer
import java.util.Locale

/** Accent-insensitive, typo-tolerant matching for the small local music library. */
object FuzzySearch {
    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    fun matches(query: String, vararg values: String?): Boolean {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return true
        val queryTokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        val candidates = values.filterNotNull()
            .flatMap { normalize(it).split(Regex("\\s+")) }
            .filter { it.isNotBlank() }
        if (candidates.isEmpty()) return false

        val allText = values.filterNotNull().joinToString(" ") { normalize(it) }
        if (allText.contains(normalizedQuery)) return true

        return queryTokens.all { wanted ->
            candidates.any { candidate -> tokenMatches(wanted, candidate) }
        }
    }

    private fun tokenMatches(query: String, candidate: String): Boolean {
        if (query == candidate || candidate.startsWith(query) || query.startsWith(candidate)) return true
        val length = query.length
        val threshold = when {
            length <= 2 -> 0
            length <= 5 -> 1
            length <= 8 -> 2
            else -> maxOf(2, length / 4)
        }
        if (threshold == 0 || kotlin.math.abs(query.length - candidate.length) > threshold) return false
        return damerauLevenshtein(query, candidate) <= threshold
    }

    /** Optimal-string-alignment distance also treats a neighbouring transposition as one typo. */
    private fun damerauLevenshtein(a: String, b: String): Int {
        val rows = a.length + 1
        val cols = b.length + 1
        val d = Array(rows) { IntArray(cols) }
        for (i in 0 until rows) d[i][0] = i
        for (j in 0 until cols) d[0][j] = j

        for (i in 1 until rows) {
            for (j in 1 until cols) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = minOf(
                    d[i - 1][j] + 1,
                    d[i][j - 1] + 1,
                    d[i - 1][j - 1] + cost,
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
                }
            }
        }
        return d[a.length][b.length]
    }
}
