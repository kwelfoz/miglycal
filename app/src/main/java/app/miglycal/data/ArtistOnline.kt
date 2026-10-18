package app.miglycal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ArtistInfo(val name: String, val imageUrl: String?, val bio: String?, val url: String?)

object ArtistOnline {
    // Public Last.fm application key: fixed in the app; never shown in Settings.
    private const val LAST_FM_API_KEY = "05334d33b73aa1d8a79d12d0dcddadfb"

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "Miglycal/1.2 (Android music player)")
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

    suspend fun info(name: String): ArtistInfo? = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext null
        val root = JSONObject(
            get("https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=${enc(name)}&api_key=$LAST_FM_API_KEY&format=json&lang=en")
        )
        if (root.has("error")) return@withContext null
        val a = root.optJSONObject("artist") ?: return@withContext null
        val images = a.optJSONArray("image")
        var image: String? = null
        if (images != null) {
            for (i in images.length() - 1 downTo 0) {
                val candidate = images.optJSONObject(i)?.optString("#text").orEmpty()
                if (candidate.startsWith("https://")) {
                    image = candidate
                    break
                }
            }
        }
        val bio = a.optJSONObject("bio")?.optString("content")
            ?.replace(Regex("<[^>]+>"), " ")
            ?.replace("&amp;", "&")
            ?.replace("&quot;", "\"")
            ?.replace("&#039;", "'")
            ?.replace("&lt;", "<")
            ?.replace("&gt;", ">")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        ArtistInfo(
            name = a.optString("name", name),
            imageUrl = image,
            bio = bio,
            url = a.optString("url").takeIf { it.startsWith("http") },
        )
    }
}
