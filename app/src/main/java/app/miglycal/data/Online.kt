package app.miglycal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Pesquisa de capas (iTunes Search = lançamentos digitais; MusicBrainz com formato
 * "Digital Media" + Cover Art Archive) e de letras (LRCLIB).
 */
object Online {

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "Miglycal/1.0 (android music player)")
        c.setRequestProperty("Accept", "application/json, image/*")
        c.connectTimeout = 10_000
        c.readTimeout = 20_000
        c.instanceFollowRedirects = true
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            return c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
    }

    private fun text(url: String) = String(get(url), Charsets.UTF_8)

    // ---------------------------------------------------------------- capas

    suspend fun covers(artist: String, album: String): List<CoverCandidate> = withContext(Dispatchers.IO) {
        val out = mutableListOf<CoverCandidate>()

        // 1) iTunes: catálogo digital
        runCatching {
            val term = enc("$artist $album".trim())
            val res = JSONObject(text("https://itunes.apple.com/search?term=$term&entity=album&limit=8"))
                .getJSONArray("results")
            for (i in 0 until res.length()) {
                val o = res.getJSONObject(i)
                val a100 = o.optString("artworkUrl100")
                if (a100.isEmpty()) continue
                val year = o.optString("releaseDate").take(4)
                out += CoverCandidate(
                    "[digital · itunes] ${o.optString("artistName")} - ${o.optString("collectionName")} ($year)",
                    a100.replace("100x100bb", "300x300bb"),
                    a100.replace("100x100bb", "1400x1400bb"),
                )
            }
        }

        // 2) MusicBrainz: primeiro releases digitais; se não houver, qualquer release
        runCatching { out += musicBrainz(artist, album, digital = true) }
        if (out.none { it.label.contains("musicbrainz") }) {
            runCatching { out += musicBrainz(artist, album, digital = false) }
        }
        out
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun musicBrainz(artist: String, album: String, digital: Boolean): List<CoverCandidate> {
        val fmt = if (digital) " AND format:\"Digital Media\"" else ""
        val artistPart = if (artist.isBlank()) "" else " AND artist:\"${esc(artist)}\""
        val q = "release:\"${esc(album)}\"$artistPart$fmt"
        val rels = JSONObject(text("https://musicbrainz.org/ws/2/release/?query=${enc(q)}&fmt=json&limit=8"))
            .optJSONArray("releases") ?: return emptyList()
        val out = mutableListOf<CoverCandidate>()
        for (i in 0 until rels.length()) {
            if (out.size >= 5) break
            val o = rels.getJSONObject(i)
            val id = o.getString("id")
            val hasArt = runCatching {
                JSONObject(text("https://coverartarchive.org/release/$id")).getJSONArray("images").length() > 0
            }.getOrDefault(false)
            if (!hasArt) continue
            val by = o.optJSONArray("artist-credit")?.optJSONObject(0)?.optString("name").orEmpty()
            val year = o.optString("date").take(4)
            val kind = if (digital) "digital" else "físico"
            out += CoverCandidate(
                "[$kind · musicbrainz] $by - ${o.optString("title")} ($year)",
                "https://coverartarchive.org/release/$id/front-250",
                "https://coverartarchive.org/release/$id/front-1200",
            )
        }
        return out
    }

    /** Descarrega a capa em alta resolução (com recurso à miniatura se falhar). */
    suspend fun downloadCover(c: CoverCandidate): ByteArray = withContext(Dispatchers.IO) {
        runCatching { get(c.fullUrl) }.getOrElse { get(c.previewUrl) }
    }

    // --------------------------------------------------------------- letras

    suspend fun lyrics(artist: String, title: String, album: String, durationMs: Long): Lyrics? =
        withContext(Dispatchers.IO) {
            val q = "artist_name=${enc(artist)}&track_name=${enc(title)}"

            runCatching {
                val u = "https://lrclib.net/api/get?$q&album_name=${enc(album)}&duration=${durationMs / 1000}"
                parseLyrics(JSONObject(text(u)))
            }.getOrNull()?.takeIf { it.synced != null || it.plain != null }?.let { return@withContext it }

            runCatching {
                val arr = JSONArray(text("https://lrclib.net/api/search?$q"))
                var best: Lyrics? = null
                for (i in 0 until arr.length()) {
                    val l = parseLyrics(arr.getJSONObject(i))
                    if (l.synced != null) return@runCatching l
                    if (best == null && l.plain != null) best = l
                }
                best
            }.getOrNull()
        }

    /** Pesquisa manual: devolve várias correspondências para o utilizador escolher. */
    suspend fun searchLyrics(artist: String, title: String): List<LyricsCandidate> = withContext(Dispatchers.IO) {
        if (artist.isBlank() && title.isBlank()) return@withContext emptyList()
        val q = "artist_name=${enc(artist)}&track_name=${enc(title)}"
        val arr = JSONArray(text("https://lrclib.net/api/search?$q"))
        val out = mutableListOf<LyricsCandidate>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val l = parseLyrics(item)
            if (l.synced.isNullOrBlank() && l.plain.isNullOrBlank()) continue
            val artistName = item.optString("artistName", artist)
            val trackName = item.optString("trackName", title)
            val albumName = item.optString("albumName").takeIf { it.isNotBlank() }
            val label = buildString {
                append(artistName).append(" — ").append(trackName)
                if (albumName != null) append(" [").append(albumName).append("]")
                append(if (l.synced != null) " · sincronizada" else " · normal")
            }
            out += LyricsCandidate(label, l)
        }
        out.distinctBy { it.label }.sortedByDescending { it.lyrics.synced != null }.take(20)
    }

    private fun parseLyrics(o: JSONObject): Lyrics {
        fun s(k: String): String? = if (o.isNull(k)) null else o.optString(k).ifBlank { null }
        return Lyrics(synced = s("syncedLyrics"), plain = s("plainLyrics"))
    }
}
