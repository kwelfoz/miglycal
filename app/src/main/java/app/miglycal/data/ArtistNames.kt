package app.miglycal.data

/**
 * Reconhece os separadores mais comuns de artistas convidados e colaborações
 * em etiquetas de áudio, sem alterar o texto original guardado no ficheiro.
 */
object ArtistNames {
    private val separators = Regex(
        """(?i)\s*(?:[,/;|]\s*|\s+&\s+|\s+\+\s+|\s+[x×]\s+|\s+(?:feat(?:uring)?\.?|ft\.?|with|vs\.?|versus|and)\s+)"""
    )

    fun split(raw: String): List<String> {
        val source = raw.trim()
        if (source.isBlank()) return emptyList()

        val parts = separators.split(source)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Remove parênteses usados apenas para envolver "feat. Artista":
        // "A1 (feat. A2)" -> ["A1", "A2"].
        return parts.map { part ->
            part.trim().trim('(', ')', '[', ']', '{', '}').trim()
        }.filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }
}
