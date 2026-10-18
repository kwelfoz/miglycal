package app.miglycal.data

/** Normaliza artistas múltiples para apresentação sem alterar as etiquetas originais. */
object ArtistNames {
    fun split(raw: String): List<String> =
        raw.split(Regex("\\s*[,/]\\s*"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
}
