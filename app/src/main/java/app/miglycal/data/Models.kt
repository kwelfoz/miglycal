package app.miglycal.data

import java.util.Locale

data class Track(
    val id: Long,
    val uri: String,
    val path: String,
    val folder: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
)

data class Playlist(
    val id: String,
    val name: String,
    val paths: List<String>,
    val shuffle: Boolean = false,
    val repeat: Int = Rep.OFF,
)

/** Modos de repetição (valores iguais aos do Media3: 0 = off, 1 = one, 2 = all). */
object Rep {
    const val OFF = 0
    const val ONE = 1
    const val ALL = 2
    fun next(r: Int) = when (r) {
        OFF -> ALL
        ALL -> ONE
        else -> OFF
    }
    fun label(r: Int) = when (r) {
        ALL -> "all"
        ONE -> "one"
        else -> "off"
    }
}

data class Tags(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val year: String = "",
    val track: String = "",
    val disc: String = "",
    val genre: String = "",
    val comment: String = "",
) {
    fun isEmpty() = listOf(title, artist, album, albumArtist, year, track, disc, genre, comment).all { it.isBlank() }
}

data class AudioInfo(
    val format: String,        // "flac" | "mp3"
    val sampleRateHz: Int,
    val bitrateKbps: Int,
    val variableBitrate: Boolean = false,
) {
    /** flac // 44.1 khz   |   mp3 // 320 kbps */
    fun label(): String = when {
        format == "flac" && sampleRateHz > 0 -> "flac // ${khz(sampleRateHz)} khz"
        format == "flac" -> "flac"
        bitrateKbps > 0 -> "$format // ${if (variableBitrate) "~" else ""}$bitrateKbps kbps"
        else -> format
    }

    private fun khz(hz: Int): String =
        String.format(Locale.US, "%.1f", hz / 1000.0).removeSuffix(".0")
}

data class CoverCandidate(val label: String, val previewUrl: String, val fullUrl: String)

data class Lyrics(val synced: String?, val plain: String?)

data class LrcLine(val ms: Long, val text: String)

data class LoadedLyrics(val synced: List<LrcLine>?, val plain: String?)
