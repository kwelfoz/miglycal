package app.miglycal.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AlbumTrackInfo(
    val name: String,
    val durationSeconds: Int,
    val position: Int,
)

data class AlbumInfo(
    val name: String,
    val artist: String,
    val imageUrl: String?,
    val releasedAt: String?,
    val genres: List<String>,
    val description: String?,
    val tracks: List<AlbumTrackInfo>,
    val url: String?,
)

/** Album metadata and track listing from Last.fm; local files always remain the source of playback. */
object AlbumOnline {
    private const val API_KEY = "05334d33b73aa1d8a79d12d0dcddadfb"

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    private fun request(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Miglycal/1.2.7 (Android music player)")
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (root.has("error")) error(root.optString("message", "Last.fm error"))
            return root
        } finally {
            connection.disconnect()
        }
    }

    private fun cleanHtml(raw: String?): String? {
        val cleaned = raw.orEmpty()
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p\\s*>"), "\n\n")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#10;", "\n")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
            .replace(Regex("\\s*Read more on Last\\.fm\\.?$", RegexOption.IGNORE_CASE), "")
            .trim()
        return cleaned.takeIf { it.isNotBlank() }
    }

    suspend fun info(artist: String, album: String): AlbumInfo? = withContext(Dispatchers.IO) {
        if (artist.isBlank() || album.isBlank()) return@withContext null
        val source = listOf(album, AlbumNames.baseTitle(album)).distinct().firstNotNullOfOrNull { candidate ->
            val url = "https://ws.audioscrobbler.com/2.0/?method=album.getinfo" +
                "&artist=${enc(artist)}&album=${enc(candidate)}&api_key=$API_KEY&format=json&autocorrect=1&lang=en"
            runCatching { request(url).optJSONObject("album") }.getOrNull()
        } ?: return@withContext null

        val images = source.optJSONArray("image")
        val image = images?.let { array ->
            (array.length() - 1 downTo 0).firstNotNullOfOrNull { index ->
                array.optJSONObject(index)?.optString("#text")
                    ?.takeIf { it.startsWith("https://") && !it.contains("2a96cbd8b46e442fc41c2b86b821562f") }
            }
        }

        val tags = source.optJSONObject("toptags")?.optJSONArray("tag") ?: JSONArray()
        val genres = (0 until tags.length()).mapNotNull { index ->
            tags.optJSONObject(index)?.optString("name")?.trim()?.takeIf { it.isNotBlank() }
        }.distinct().take(8)

        val tracksRoot = source.optJSONObject("tracks")?.opt("track")
        val trackArray = when (tracksRoot) {
            is JSONArray -> tracksRoot
            is JSONObject -> JSONArray().put(tracksRoot)
            else -> JSONArray()
        }
        val tracks = (0 until trackArray.length()).mapNotNull { index ->
            val item = trackArray.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name").trim()
            if (name.isBlank()) return@mapNotNull null
            val rank = item.optJSONObject("@attr")?.optString("rank")?.toIntOrNull() ?: (index + 1)
            AlbumTrackInfo(
                name = name,
                durationSeconds = item.optString("duration").toIntOrNull() ?: 0,
                position = rank,
            )
        }.sortedBy { it.position }

        val wiki = source.optJSONObject("wiki")
        AlbumInfo(
            name = source.optString("name").ifBlank { album },
            artist = source.optString("artist").ifBlank { artist },
            imageUrl = image,
            releasedAt = source.optString("releasedate").trim().takeIf { it.isNotBlank() },
            genres = genres,
            description = cleanHtml(wiki?.optString("content")?.takeIf { it.isNotBlank() } ?: wiki?.optString("summary")),
            tracks = tracks,
            url = source.optString("url").takeIf { it.startsWith("http") },
        )
    }
}
