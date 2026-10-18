package app.miglycal.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.miglycal.data.Rep
import app.miglycal.data.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlayerState(
    val path: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val playing: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Int = Rep.OFF,
    val durationMs: Long = 0,
)

/** Ponte entre a interface e o serviço de reprodução (MediaController). */
class PlayerController(private val ctx: Context) {
    private var future: ListenableFuture<MediaController>? = null
    private var mc: MediaController? = null
    private var pending: (() -> Unit)? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = push()
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).buildAsync()
        future = f
        f.addListener({
            val c = runCatching { f.get() }.getOrNull() ?: return@addListener
            mc = c
            c.addListener(listener)
            push()
            pending?.invoke()
            pending = null
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun push() {
        val p = mc ?: return
        val m = p.currentMediaItem
        _state.value = PlayerState(
            path = m?.mediaId?.takeIf { it.isNotEmpty() },
            title = m?.mediaMetadata?.title?.toString().orEmpty(),
            artist = m?.mediaMetadata?.artist?.toString().orEmpty(),
            album = m?.mediaMetadata?.albumTitle?.toString().orEmpty(),
            playing = p.isPlaying,
            shuffle = p.shuffleModeEnabled,
            repeat = p.repeatMode,
            durationMs = p.duration.takeIf { it != C.TIME_UNSET } ?: 0L,
        )
    }

    private fun withMc(block: (MediaController) -> Unit) {
        val c = mc
        if (c != null) block(c) else pending = { mc?.let(block) }
    }

    private fun item(t: Track) = MediaItem.Builder()
        .setMediaId(t.path)
        .setUri(Uri.parse(t.uri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(t.title)
                .setArtist(t.artist)
                .setAlbumTitle(t.album)
                .build()
        )
        .build()

    fun play(list: List<Track>, start: Int, shuffle: Boolean, repeat: Int) = withMc { p ->
        if (list.isEmpty()) return@withMc
        p.shuffleModeEnabled = shuffle
        p.repeatMode = repeat
        p.setMediaItems(list.map(::item), start.coerceIn(0, list.lastIndex), 0L)
        p.prepare()
        p.play()
    }

    fun toggle() = withMc { if (it.isPlaying) it.pause() else it.play() }
    fun next() = withMc { it.seekToNext() }
    fun prev() = withMc { it.seekToPrevious() }
    fun setShuffle(v: Boolean) = withMc { it.shuffleModeEnabled = v }
    fun setRepeat(v: Int) = withMc { it.repeatMode = v }
    fun seekTo(ms: Long) = withMc { it.seekTo(ms.coerceAtLeast(0)) }

    fun seekFraction(f: Float) = withMc {
        val d = it.duration
        if (d != C.TIME_UNSET && d > 0) it.seekTo((d * f.coerceIn(0f, 1f)).toLong())
    }

    fun position(): Long = mc?.currentPosition ?: 0L

    fun release() {
        mc?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        mc = null
    }
}
