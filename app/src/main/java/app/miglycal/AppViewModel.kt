package app.miglycal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.miglycal.data.Library
import app.miglycal.data.Playlist
import app.miglycal.data.Rep
import app.miglycal.data.Store
import app.miglycal.data.Track
import app.miglycal.player.PlayerController
import app.miglycal.ui.Accent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val player = PlayerController(app)

    private val all = MutableStateFlow<List<Track>>(emptyList())
    val blocked = MutableStateFlow(store.blocked)
    val accent = MutableStateFlow(Accent.from(store.accent))
    val musicFolders = MutableStateFlow(store.musicFolders)
    val setupCompleted = MutableStateFlow(store.setupCompleted)
    val playlists = MutableStateFlow(store.loadPlaylists())
    val loading = MutableStateFlow(false)

    /** Faixas visíveis (sem as pastas bloqueadas). */
    val tracks: StateFlow<List<Track>> = combine(all, blocked, musicFolders) { a, b, folders ->
        a.filter { t ->
            folders.any { root ->
                val normalized = root.trimEnd('/')
                t.path == normalized || t.path.startsWith("$normalized/")
            } && b.none { f -> t.folder == f || t.path.startsWith("$f/") }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allByPath: StateFlow<Map<String, Track>> =
        tracks.map { l -> l.associateBy { it.path } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            val list = withContext(Dispatchers.IO) {
                runCatching { Library.scan(getApplication()) }.getOrDefault(emptyList())
            }
            all.value = list
            loading.value = false
        }
    }

    // ------------------------------------------------------------ definições

    fun addMusicFolder(path: String) {
        val normalized = path.trimEnd('/').ifBlank { "/" }
        val next = musicFolders.value + normalized
        store.musicFolders = next
        musicFolders.value = next
    }

    fun removeMusicFolder(path: String) {
        val next = musicFolders.value - path
        store.musicFolders = next
        musicFolders.value = next
    }

    fun completeSetup() {
        if (musicFolders.value.isEmpty()) return
        store.setupCompleted = true
        setupCompleted.value = true
        refresh()
    }

    fun setAccent(a: Accent) {
        store.accent = a.key
        accent.value = a
    }

    fun block(folder: String) {
        val s = blocked.value + folder
        store.blocked = s
        blocked.value = s
    }

    fun unblock(folder: String) {
        val s = blocked.value - folder
        store.blocked = s
        blocked.value = s
    }

    // ------------------------------------------------------------- playlists

    private fun savePl(l: List<Playlist>) {
        playlists.value = l
        store.savePlaylists(l)
    }

    fun createPlaylist(name: String, paths: List<String> = emptyList()): String {
        val p = Playlist(UUID.randomUUID().toString(), name.trim().ifBlank { "sem nome" }, paths.distinct())
        savePl(playlists.value + p)
        return p.id
    }

    fun addToPlaylist(id: String, paths: List<String>) =
        savePl(playlists.value.map { if (it.id == id) it.copy(paths = (it.paths + paths).distinct()) else it })

    fun removeFromPlaylist(id: String, path: String) =
        savePl(playlists.value.map { if (it.id == id) it.copy(paths = it.paths - path) else it })

    fun renamePlaylist(id: String, name: String) =
        savePl(playlists.value.map { if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it })

    fun deletePlaylist(id: String) = savePl(playlists.value.filterNot { it.id == id })

    fun setPlaylistShuffle(id: String, v: Boolean) =
        savePl(playlists.value.map { if (it.id == id) it.copy(shuffle = v) else it })

    fun cyclePlaylistRepeat(id: String) =
        savePl(playlists.value.map { if (it.id == id) it.copy(repeat = Rep.next(it.repeat)) else it })

    // ------------------------------------------------------------ reprodução

    fun playTracks(list: List<Track>, index: Int) =
        player.play(list, index, store.shuffle, store.repeat)

    fun playShuffled(list: List<Track>) {
        if (list.isEmpty()) return
        store.shuffle = true
        player.play(list, list.indices.random(), true, store.repeat)
    }

    fun playPlaylist(pl: Playlist, startPath: String? = null, forceShuffle: Boolean = false) {
        val list = pl.paths.mapNotNull { allByPath.value[it] }
        if (list.isEmpty()) return
        val shuffle = pl.shuffle || forceShuffle
        val idx = when {
            startPath != null -> list.indexOfFirst { it.path == startPath }.coerceAtLeast(0)
            shuffle -> list.indices.random()
            else -> 0
        }
        player.play(list, idx, shuffle, pl.repeat)
    }

    fun toggleShuffle() {
        val v = !player.state.value.shuffle
        store.shuffle = v
        player.setShuffle(v)
    }

    fun cycleRepeat() {
        val n = Rep.next(player.state.value.repeat)
        store.repeat = n
        player.setRepeat(n)
    }

    override fun onCleared() {
        player.release()
    }
}
