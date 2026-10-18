package app.miglycal.data

import org.json.JSONObject
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ArtistInfo(val name: String, val imageUrl: String?, val bio: String?, val url: String?)
data class ArtistRelease(val title: String, val year: String, val type: String, val coverUrl: String?, val url: String?)

object ArtistOnline {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "Miglycal/1.1 (Android music library)")
        c.setRequestProperty("Accept", "application/json")
        c.connectTimeout = 10000
        c.readTimeout = 15000
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    suspend fun info(name: String, apiKey: String): ArtistInfo? = withContext(Dispatchers.IO) {
        if (name.isBlank() || apiKey.isBlank()) return@withContext null
        val root = JSONObject(get("https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=${enc(name)}&api_key=${enc(apiKey)}&format=json&lang=en"))
        if (root.has("error")) return@withContext null
        val a = root.optJSONObject("artist") ?: return@withContext null
        val images = a.optJSONArray("image")
        var image: String? = null
        if (images != null) for (i in images.length() - 1 downTo 0) {
            val candidate = images.optJSONObject(i)?.optString("#text").orEmpty()
            if (candidate.startsWith("https://")) { image = candidate; break }
        }
        val bio = a.optJSONObject("bio")?.optString("content")?.replace(Regex("<[^>]+>"), "")?.trim()?.takeIf { it.isNotBlank() }
        ArtistInfo(a.optString("name", name), image, bio, a.optString("url").takeIf { it.startsWith("http") })
    }

    suspend fun releases(name: String): List<ArtistRelease> = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext emptyList()
        val artists = JSONObject(get("https://musicbrainz.org/ws/2/artist/?query=artist:${enc(name)}&fmt=json&limit=5"))
            .optJSONArray("artists") ?: return@withContext emptyList()
        if (artists.length() == 0) return@withContext emptyList()
        val mbid = artists.optJSONObject(0)?.optString("id").orEmpty()
        if (mbid.isBlank()) return@withContext emptyList()
        val out = mutableListOf<ArtistRelease>()
        for ((type, label) in listOf("album" to "ÁLBUM", "single" to "SINGLE")) {
            runCatching {
                val groups = JSONObject(get("https://musicbrainz.org/ws/2/release-group/?artist=${enc(mbid)}&type=${type}&fmt=json&limit=50"))
                    .optJSONArray("release-groups") ?: return@runCatching
                for (i in 0 until groups.length()) {
                    val g = groups.optJSONObject(i) ?: continue
                    val title = g.optString("title").takeIf { it.isNotBlank() } ?: continue
                    val id = g.optString("id")
                    val date = g.optString("first-release-date")
                    val year = date.take(4)
                    val cover = if (id.isNotBlank()) "https://coverartarchive.org/release-group/$id/front-250" else null
                    out += ArtistRelease(title, year, label, cover, if (id.isNotBlank()) "https://musicbrainz.org/release-group/$id" else null)
                }
            }
        }
        out.distinctBy { it.type + "|" + it.title.lowercase() }.sortedWith(compareBy<ArtistRelease> { it.type }.thenByDescending { it.year }.thenBy { it.title.lowercase() })
    }
}
