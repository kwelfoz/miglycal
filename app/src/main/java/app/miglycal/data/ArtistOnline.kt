package app.miglycal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ArtistInfo(val name: String, val imageUrl: String?, val bio: String?, val url: String?)

object ArtistOnline {
    // Public Last.fm application key: fixed in the app; never shown in Settings.
    private const val LAST_FM_API_KEY = "05334d33b73aa1d8a79d12d0dcddadfb"
    private const val LAST_FM_DEFAULT_AVATAR = "2a96cbd8b46e442fc41c2b86b821562f"

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "Miglycal/1.2.4 (Android music player)")
        c.setRequestProperty("Accept", "application/json")
        c.connectTimeout = 10000
        c.readTimeout = 15000
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun deezerArtist(name: String): JSONObject? = runCatching {
        val root = JSONObject(get("https://api.deezer.com/search/artist?q=${enc(name)}"))
        val data = root.optJSONArray("data") ?: JSONArray()
        (0 until data.length())
            .mapNotNull { data.optJSONObject(it) }
            .firstOrNull { it.optString("name").trim().equals(name.trim(), ignoreCase = true) }
    }.getOrNull()

    private fun lastFmImage(artist: JSONObject): String? {
        val images = artist.optJSONArray("image") ?: return null
        for (i in images.length() - 1 downTo 0) {
            val raw = images.optJSONObject(i)?.optString("#text").orEmpty().trim()
            if (!raw.startsWith("http", ignoreCase = true)) continue
            if (raw.contains(LAST_FM_DEFAULT_AVATAR, ignoreCase = true)) continue
            return raw.replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
        }
        return null
    }

    suspend fun info(name: String): ArtistInfo? = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext null

        // Last.fm provides the biography; its artist images can be generic placeholders,
        // so fall back to Deezer's artist pictures when needed.
        val lastRoot = runCatching {
            JSONObject(
                get("https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=${enc(name)}&api_key=$LAST_FM_API_KEY&format=json&lang=en&autocorrect=1")
            )
        }.getOrNull()
        val lastArtist = lastRoot?.optJSONObject("artist")
        val deezer = deezerArtist(name)

        val deezerImage = deezer?.optString("picture_xl").orEmpty()
            .takeIf { it.startsWith("https://") && !it.endsWith("/500x500-000000-80-0-0.jpg") }
            ?: deezer?.optString("picture_big").orEmpty().takeIf { it.startsWith("https://") }
        val image = deezerImage ?: lastArtist?.let(::lastFmImage)

        val bio = lastArtist?.optJSONObject("bio")?.optString("content")
            ?.replace(Regex("<[^>]+>"), " ")
            ?.replace("&amp;", "&")
            ?.replace("&quot;", "\"")
            ?.replace("&#039;", "'")
            ?.replace("&lt;", "<")
            ?.replace("&gt;", ">")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (lastArtist == null && deezer == null) return@withContext null
        ArtistInfo(
            name = lastArtist?.optString("name")?.takeIf { it.isNotBlank() }
                ?: deezer?.optString("name")?.takeIf { it.isNotBlank() }
                ?: name,
            imageUrl = image,
            bio = bio,
            url = lastArtist?.optString("url")?.takeIf { it.startsWith("http") },
        )
    }
}
