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

    /**
     * Finds complete track tags from MusicBrainz. Digital releases are searched first;
     * physical releases are used only when no matching digital release is found.
     */
    suspend fun searchTags(artist: String, title: String, album: String): List<TagCandidate> =
        withContext(Dispatchers.IO) {
            if (title.isBlank()) return@withContext emptyList()

            fun norm(value: String) = value.lowercase().filter { it.isLetterOrDigit() }

            suspend fun findPass(digitalOnly: Boolean): List<TagCandidate> {
                val releaseIds = linkedSetOf<String>()
                if (album.isNotBlank()) {
                    val format = if (digitalOnly) " AND format:\"Digital Media\"" else ""
                    val artistPart = if (artist.isBlank()) "" else " AND artist:\"${esc(artist)}\""
                    val q = "release:\"${esc(album)}\"$artistPart$format"
                    runCatching {
                        val releases = JSONObject(
                            text("https://musicbrainz.org/ws/2/release/?query=${enc(q)}&fmt=json&limit=10")
                        ).optJSONArray("releases") ?: return@runCatching
                        for (i in 0 until releases.length()) {
                            releases.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }?.let(releaseIds::add)
                        }
                    }
                }

                // If the album tag is missing or the album search didn't return anything,
                // resolve the recording and inspect its releases.
                if (releaseIds.isEmpty()) {
                    val artistPart = if (artist.isBlank()) "" else " AND artist:\"${esc(artist)}\""
                    val q = "recording:\"${esc(title)}\"$artistPart"
                    runCatching {
                        val recordings = JSONObject(
                            text("https://musicbrainz.org/ws/2/recording/?query=${enc(q)}&fmt=json&limit=8")
                        ).optJSONArray("recordings") ?: return@runCatching
                        for (i in 0 until recordings.length()) {
                            val rec = recordings.optJSONObject(i) ?: continue
                            if (norm(rec.optString("title")) != norm(title)) continue
                            val releases = rec.optJSONArray("releases") ?: continue
                            for (j in 0 until releases.length()) {
                                val rel = releases.optJSONObject(j) ?: continue
                                val releaseTitle = rel.optString("title")
                                if (album.isNotBlank() && norm(releaseTitle) != norm(album)) continue
                                rel.optString("id").takeIf { it.isNotBlank() }?.let(releaseIds::add)
                                if (releaseIds.size >= 10) break
                            }
                            if (releaseIds.size >= 10) break
                        }
                    }
                }

                val matches = mutableListOf<TagCandidate>()
                for (releaseId in releaseIds.take(10)) {
                    val release = runCatching {
                        JSONObject(text("https://musicbrainz.org/ws/2/release/$releaseId?inc=recordings+artist-credits+genres&fmt=json"))
                    }.getOrNull() ?: continue

                    val media = release.optJSONArray("media") ?: continue
                    val digitalRelease = (0 until media.length()).any { i ->
                        media.optJSONObject(i)?.optString("format").orEmpty().contains("digital", ignoreCase = true)
                    }
                    if (digitalOnly && !digitalRelease) continue

                    val releaseTitle = release.optString("title").ifBlank { album }
                    if (album.isNotBlank() && norm(releaseTitle) != norm(album)) continue
                    val year = release.optString("date").take(4)
                    val releaseArtist = buildString {
                        val credits = release.optJSONArray("artist-credit")
                        if (credits != null) {
                            for (i in 0 until credits.length()) {
                                val part = credits.optJSONObject(i) ?: continue
                                append(part.optString("name"))
                                append(part.optString("joinphrase"))
                            }
                        }
                    }.trim().ifBlank { artist }

                    for (m in 0 until media.length()) {
                        val medium = media.optJSONObject(m) ?: continue
                        val tracks = medium.optJSONArray("tracks") ?: continue
                        for (i in 0 until tracks.length()) {
                            val track = tracks.optJSONObject(i) ?: continue
                            val recording = track.optJSONObject("recording")
                            val trackTitle = track.optString("title").ifBlank { recording?.optString("title").orEmpty() }
                            if (norm(trackTitle) != norm(title)) continue
                            val trackArtist = buildString {
                                val credits = track.optJSONArray("artist-credit") ?: recording?.optJSONArray("artist-credit")
                                if (credits != null) for (j in 0 until credits.length()) {
                                    val part = credits.optJSONObject(j) ?: continue
                                    append(part.optString("name"))
                                    append(part.optString("joinphrase"))
                                }
                            }.trim().ifBlank { releaseArtist }
                            val trackNumber = track.optString("number").ifBlank { track.optString("position") }
                            val discNumber = medium.optString("position").ifBlank { (m + 1).toString() }
                            val genreArray = recording?.optJSONArray("genres") ?: release.optJSONArray("genres")
                            val genres = genreArray?.let { genreList ->
                                (0 until genreList.length()).mapNotNull { genreList.optJSONObject(it)?.optString("name") }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                    .joinToString("; ")
                            }.orEmpty()
                            val completeTags = Tags(
                                title = trackTitle,
                                artist = trackArtist,
                                album = releaseTitle,
                                albumArtist = releaseArtist,
                                year = year,
                                track = trackNumber,
                                disc = discNumber,
                                genre = genres,
                            )
                            val releaseKind = if (digitalRelease) "digital" else "physical/other"
                            val label = "[$releaseKind] $releaseArtist — $releaseTitle" +
                                (if (year.isNotBlank()) " ($year)" else "") +
                                (if (trackNumber.isNotBlank()) " · track $trackNumber" else "")
                            matches += TagCandidate(label, completeTags, digitalRelease)
                        }
                    }
                }
                return matches.distinctBy { listOf(it.tags.artist, it.tags.album, it.tags.title, it.tags.year, it.tags.track).joinToString("|") }
            }

            val digital = runCatching { findPass(digitalOnly = true) }.getOrDefault(emptyList())
            if (digital.isNotEmpty()) digital
            else runCatching { findPass(digitalOnly = false) }.getOrDefault(emptyList()).take(20)
        }

    private fun parseLyrics(o: JSONObject): Lyrics {
        fun s(k: String): String? = if (o.isNull(k)) null else o.optString(k).ifBlank { null }
        return Lyrics(synced = s("syncedLyrics"), plain = s("plainLyrics"))
    }
}
