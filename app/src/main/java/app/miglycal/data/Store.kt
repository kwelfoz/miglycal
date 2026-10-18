package app.miglycal.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Definições e playlists, guardadas localmente. */
class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("miglycal", Context.MODE_PRIVATE)

    var accent: String
        get() = sp.getString("accent", "green") ?: "green"
        set(v) = sp.edit().putString("accent", v).apply()

    var blocked: Set<String>
        get() = (sp.getStringSet("blocked", emptySet()) ?: emptySet()).toSet()
        set(v) = sp.edit().putStringSet("blocked", v.toSet()).apply()

    var shuffle: Boolean
        get() = sp.getBoolean("shuffle", false)
        set(v) = sp.edit().putBoolean("shuffle", v).apply()

    var repeat: Int
        get() = sp.getInt("repeat", Rep.OFF)
        set(v) = sp.edit().putInt("repeat", v).apply()

    fun loadPlaylists(): List<Playlist> = runCatching {
        val arr = JSONArray(sp.getString("playlists", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ps = o.getJSONArray("paths")
            Playlist(
                id = o.getString("id"),
                name = o.getString("name"),
                paths = (0 until ps.length()).map { ps.getString(it) },
                shuffle = o.optBoolean("shuffle", false),
                repeat = o.optInt("repeat", Rep.OFF),
            )
        }
    }.getOrDefault(emptyList())

    fun savePlaylists(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("paths", JSONArray(p.paths))
                    .put("shuffle", p.shuffle)
                    .put("repeat", p.repeat)
            )
        }
        sp.edit().putString("playlists", arr.toString()).apply()
    }
}
