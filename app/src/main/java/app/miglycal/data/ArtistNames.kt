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

        // Só remove parênteses de limite quando a etiqueta foi dividida.
        // Assim, nomes individuais como "Artista (Live)" ficam intactos.
        val cleaned = if (parts.size > 1) {
            parts.map { it.trim().trim('(', ')', '[', ']', '{', '}').trim() }
        } else {
            parts
        }

        return cleaned.filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }
}
