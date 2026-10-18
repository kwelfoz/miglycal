package app.miglycal.data

import java.io.File

/** Letras locais: ficheiro .lrc ao lado da música (sincronizada) ou campo LYRICS da etiqueta. */
object LyricsStore {
    private val ts = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?\]""")

    fun lrcFile(path: String) = File(path.substringBeforeLast('.') + ".lrc")

    fun parse(lrc: String): List<LrcLine> {
        val out = mutableListOf<LrcLine>()
        for (raw in lrc.lines()) {
            val stamps = ts.findAll(raw).toList()
            if (stamps.isEmpty()) continue
            val text = raw.substring(stamps.last().range.last + 1).trim()
            for (m in stamps) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = if (frac.isEmpty()) 0L else frac.padEnd(3, '0').take(3).toLong()
                out += LrcLine(min * 60_000 + sec * 1_000 + ms, text)
            }
        }
        return out.sortedBy { it.ms }
    }

    /** Remove os carimbos de tempo de uma letra sincronizada. */
    fun stripTimestamps(lrc: String): String =
        lrc.lines().joinToString("\n") { ts.replace(it, "").trim() }

    fun load(path: String): LoadedLyrics {
        val f = lrcFile(path)
        if (f.exists()) {
            val synced = runCatching { parse(f.readText()) }.getOrNull()
            if (!synced.isNullOrEmpty()) return LoadedLyrics(synced, null)
        }
        val tagText = TagIO.readLyrics(path)
        if (!tagText.isNullOrBlank()) {
            val synced = parse(tagText)
            return if (synced.size > 3) LoadedLyrics(synced, null) else LoadedLyrics(null, tagText)
        }
        return LoadedLyrics(null, null)
    }
}
