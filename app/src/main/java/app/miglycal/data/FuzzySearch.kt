package app.miglycal.data

import java.text.Normalizer
import java.util.Locale

/** Accent-insensitive, typo-tolerant search with reusable indexes for local library data. */
object FuzzySearch {
    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
    }

    private fun matchesPrepared(queryTokens: List<String>, fullText: String, candidates: List<String>): Boolean {
        if (queryTokens.isEmpty()) return true
        val normalizedQuery = queryTokens.joinToString(" ")
        if (fullText.contains(normalizedQuery)) return true
        return queryTokens.all { wanted -> candidates.any { tokenMatches(wanted, it) } }
    }

    /** Build once after a library, folder or playlist changes; search stays cheap per keystroke. */
    fun <T> index(items: List<T>, fields: (T) -> Iterable<String?>): Index<T> = Index(items, fields)

    class Index<T> internal constructor(items: List<T>, fields: (T) -> Iterable<String?>) {
        private data class Entry<T>(val item: T, val fullText: String, val tokens: List<String>)
        private val entries: List<Entry<T>> = items.map { item ->
            val prepared = fields(item).filterNotNull().map(FuzzySearch::normalize).filter { it.isNotBlank() }
            Entry(item, prepared.joinToString(" "), prepared.flatMap { it.split(Regex("\\s+")) }.distinct())
        }

        fun search(query: String): List<T> {
            val normalized = FuzzySearch.normalize(query)
            if (normalized.isBlank()) return entries.map { it.item }
            val wanted = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
            return entries.asSequence()
                .filter { FuzzySearch.matchesPrepared(wanted, it.fullText, it.tokens) }
                .map { it.item }
                .toList()
        }
    }

    fun matches(query: String, vararg values: String?): Boolean {
        val normalized = normalize(query)
        if (normalized.isBlank()) return true
        val prepared = values.filterNotNull().map(::normalize).filter { it.isNotBlank() }
        val candidates = prepared.flatMap { it.split(Regex("\\s+")) }.distinct()
        return matchesPrepared(normalized.split(Regex("\\s+")), prepared.joinToString(" "), candidates)
    }

    private fun tokenMatches(query: String, candidate: String): Boolean {
        if (query == candidate || candidate.startsWith(query) || query.startsWith(candidate)) return true
        val threshold = when {
            query.length <= 2 -> 0
            query.length <= 5 -> 1
            query.length <= 8 -> 2
            else -> maxOf(2, query.length / 4)
        }
        if (threshold == 0 || kotlin.math.abs(query.length - candidate.length) > threshold) return false
        return withinDistance(query, candidate, threshold)
    }

    /** Bounded Damerau-Levenshtein: keeps transposition matching without allocating a matrix. */
    private fun withinDistance(a: String, b: String, limit: Int): Boolean {
        if (kotlin.math.abs(a.length - b.length) > limit) return false
        var twoBack = IntArray(b.length + 1) { it }
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            var rowMin = current[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = minOf(value, twoBack[j - 2] + 1)
                }
                current[j] = value
                rowMin = minOf(rowMin, value)
            }
            if (rowMin > limit) return false
            val swap = twoBack
            twoBack = previous
            previous = current
            current = swap
        }
        return previous[b.length] <= limit
    }
}
