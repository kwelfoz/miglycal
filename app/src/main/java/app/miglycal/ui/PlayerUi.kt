package app.miglycal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miglycal.AppViewModel
import app.miglycal.data.AudioInfo
import app.miglycal.data.LoadedLyrics
import app.miglycal.data.LyricsStore
import app.miglycal.data.Rep
import app.miglycal.data.TagIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Posição de reprodução atualizada em intervalos curtos. */
@Composable
fun rememberPosition(vm: AppViewModel, playing: Boolean): State<Long> =
    produceState(0L, playing) {
        while (true) {
            value = vm.player.position()
            delay(if (playing) 250L else 1000L)
        }
    }

// ------------------------------------------------------------------ mini leitor

@Composable
fun MiniPlayer(vm: AppViewModel, onOpen: () -> Unit) {
    val p = LocalPal.current
    val st by vm.player.state.collectAsState()
    val pos by rememberPosition(vm, st.playing)
    if (st.path == null) return

    val frac = if (st.durationMs > 0) (pos.toFloat() / st.durationMs).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().background(p.surface).clickable(onClick = onOpen)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(p.line)) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(p.accent))
        }
        Row(
            Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverImage(st.path, Modifier.size(40.dp))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Txt(st.title, bold = true, maxLines = 1)
                Txt(st.artist, color = p.dim, size = 11.sp, maxLines = 1)
            }
            Btn("|<", { vm.player.prev() })
            Btn(if (st.playing) "pause" else "play", { vm.player.toggle() }, active = true)
            Btn(">|", { vm.player.next() })
        }
    }
}

// ----------------------------------------------------------------- leitor grande

@Composable
fun FullPlayer(vm: AppViewModel, onClose: () -> Unit, onTags: (String) -> Unit) {
    val p = LocalPal.current
    val st by vm.player.state.collectAsState()
    val pos by rememberPosition(vm, st.playing)
    var showLyrics by remember { mutableStateOf(false) }
    val path = st.path
    val info by produceState<AudioInfo?>(null, path) {
        value = if (path == null) null else withContext(Dispatchers.IO) { TagIO.info(path) }
    }
    val frac = if (st.durationMs > 0) (pos.toFloat() / st.durationMs).coerceIn(0f, 1f) else 0f

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .pointerInput(Unit) { detectTapGestures { } } // não deixa cliques passarem para trás
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Btn("v", onClose)
            Txt("a tocar", color = p.dim, size = 11.sp, modifier = Modifier.weight(1f), align = TextAlign.Center)
            Btn("letra", { showLyrics = !showLyrics }, active = showLyrics)
            Btn("tags", { path?.let(onTags) })
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (showLyrics && path != null) {
                LyricsView(path, pos) { vm.player.seekTo(it) }
            } else {
                CoverImage(path, Modifier.aspectRatio(1f))
            }
        }

        Spacer(Modifier.height(14.dp))
        Txt(st.title, size = 20.sp, bold = true, maxLines = 2)
        Txt(st.artist, color = p.accent, size = 14.sp, maxLines = 1)
        Txt(st.album, color = p.dim, size = 12.sp, maxLines = 1)
        Spacer(Modifier.height(12.dp))

        TermBar(frac, onSeek = { vm.player.seekFraction(it) })
        Row(Modifier.fillMaxWidth()) {
            Txt(fmt(pos), color = p.dim, size = 11.sp)
            Spacer(Modifier.weight(1f))
            Txt(fmt(st.durationMs), color = p.dim, size = 11.sp)
        }
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Btn("shuf", { vm.toggleShuffle() }, active = st.shuffle)
            Btn("|<", { vm.player.prev() })
            Btn(if (st.playing) "pause" else "play", { vm.player.toggle() }, active = true, size = 15.sp)
            Btn(">|", { vm.player.next() })
            Btn("rep:${Rep.label(st.repeat)}", { vm.cycleRepeat() }, active = st.repeat != Rep.OFF)
        }

        Spacer(Modifier.height(16.dp))
        Txt(
            info?.label().orEmpty(),
            color = p.dim,
            size = 10.sp,
            align = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        )
    }
}

// ------------------------------------------------------------------------ letras

@Composable
fun LyricsView(path: String, pos: Long, onSeek: (Long) -> Unit) {
    val p = LocalPal.current
    val lyr by produceState<LoadedLyrics?>(null, path) {
        value = withContext(Dispatchers.IO) { LyricsStore.load(path) }
    }
    val l = lyr
    when {
        l == null -> Txt("> a carregar letra...", color = p.dim)
        l.synced != null -> {
            val lines = l.synced
            val idx = lines.indexOfLast { it.ms <= pos }.coerceAtLeast(0)
            val state = rememberLazyListState()
            LaunchedEffect(idx, path) { state.animateScrollToItem((idx - 2).coerceAtLeast(0)) }
            LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(lines) { i, ln ->
                    Txt(
                        ln.text.ifBlank { "..." },
                        color = if (i == idx) p.accent else p.dim,
                        bold = i == idx,
                        size = 15.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onSeek(ln.ms) }.padding(vertical = 6.dp),
                    )
                }
            }
        }
        l.plain != null -> {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Txt(l.plain, size = 14.sp)
            }
        }
        else -> Txt("> sem letra. abre [tags] para procurar online.", color = p.dim)
    }
}
