package app.miglycal.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.miglycal.AppViewModel
import app.miglycal.BuildConfig
import app.miglycal.data.Library
import app.miglycal.data.Rep
import app.miglycal.data.Track

// ---------------------------------------------------------------- permissões

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    val p = LocalPal.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("miglycal", size = 30.sp, bold = true, color = p.accent)
            BlinkCursor(30.sp)
        }
        Spacer(Modifier.size(16.dp))
        Txt("> o miglycal precisa de acesso ao áudio do telemóvel para listar a tua música (flac e mp3).", color = p.dim)
        Spacer(Modifier.size(16.dp))
        Btn("dar acesso", onRequest, active = true)
    }
}

// ----------------------------------------------------------------- biblioteca

@Composable
fun LibraryScreen(
    vm: AppViewModel,
    openFolder: (String) -> Unit,
    openTags: (Track) -> Unit,
    addToPlaylist: (List<Track>) -> Unit,
    openArtist: (String) -> Unit = {},
) {
    val p = LocalPal.current
    val tracks by vm.tracks.collectAsState()
    val loading by vm.loading.collectAsState()
    val st by vm.player.state.collectAsState()
    var view by rememberSaveable { mutableStateOf("pastas") }
    var blockAsk by remember { mutableStateOf<String?>(null) }
    val folders = remember(tracks) { tracks.groupBy { it.folder }.entries.sortedBy { it.key.lowercase() } }
    val sorted = remember(tracks) { tracks.sortedBy { it.title.lowercase() } }

    Column(Modifier.fillMaxSize()) {
        Prompt("biblioteca") { Btn(if (loading) "..." else "rescan", { vm.refresh() }) }
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Btn("pastas", { view = "pastas" }, active = view == "pastas")
            Btn("tudo", { view = "tudo" }, active = view == "tudo")
            Spacer(Modifier.weight(1f))
            Txt(
                "${tracks.size} faixas · ${folders.size} pastas",
                color = p.dim, size = 11.sp, modifier = Modifier.padding(end = 6.dp),
            )
        }
        Rule()
        if (tracks.isEmpty()) {
            Txt(
                if (loading) "> a procurar música..." else "> nenhuma faixa flac/mp3 encontrada.",
                color = p.dim, modifier = Modifier.padding(16.dp),
            )
        } else if (view == "pastas") {
            LazyColumn(Modifier.fillMaxSize()) {
                items(folders, key = { it.key }) { entry ->
                    val folder = entry.key
                    Row(
                        Modifier.fillMaxWidth().clickable { openFolder(folder) }.padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                            Txt(Library.shortPath(folder), color = p.accent, maxLines = 1)
                            Txt("${entry.value.size} faixas", color = p.dim, size = 11.sp)
                        }
                        Btn("bloquear", { blockAsk = folder }, size = 11.sp)
                    }
                    Rule()
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(sorted, key = { _, t -> t.path }) { i, t ->
                    TrackRow(
                        label = "",
                        track = t,
                        playing = st.path == t.path,
                        onClick = { vm.playTracks(sorted, i) },
                        menu = listOf(
                            "adicionar à playlist" to { addToPlaylist(listOf(t)) },
                            "editar etiquetas" to { openTags(t) },
                        ),
                        onArtistClick = openArtist,
                    )
                }
            }
        }
    }

    blockAsk?.let { f ->
        ConfirmDialog(
            title = "bloquear pasta",
            body = "${Library.shortPath(f)}\n\nas faixas desta pasta (e subpastas) deixam de aparecer na biblioteca. podes desbloquear nas definições.",
            okLabel = "bloquear",
            onOk = { vm.block(f); blockAsk = null },
            onDismiss = { blockAsk = null },
        )
    }
}

@Composable
fun FolderScreen(
    vm: AppViewModel,
    folder: String,
    back: () -> Unit,
    openTags: (Track) -> Unit,
    addToPlaylist: (List<Track>) -> Unit,
    openArtist: (String) -> Unit = {},
) {
    val p = LocalPal.current
    val all by vm.tracks.collectAsState()
    val st by vm.player.state.collectAsState()
    val list = remember(all, folder) { all.filter { it.folder == folder }.sortedBy { it.path.lowercase() } }
    var selecting by remember { mutableStateOf(false) }
    var sel by remember { mutableStateOf(setOf<String>()) }
    var blockAsk by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Prompt(Library.shortPath(folder)) { Btn("voltar", back) }
        Flow(Modifier.padding(horizontal = 10.dp)) {
            if (!selecting) {
                Btn("tocar", { vm.playTracks(list, 0) }, enabled = list.isNotEmpty())
                Btn("aleat.", { vm.playShuffled(list) }, enabled = list.isNotEmpty())
                Btn("selecionar", { selecting = true; sel = emptySet() })
            } else {
                val allSelected = list.isNotEmpty() && sel.size == list.size
                Btn(if (allSelected) "limpar" else "todas", {
                    sel = if (allSelected) emptySet() else list.map { it.path }.toSet()
                })
                Btn("fila", {
                    vm.queueTracks(list.filter { it.path in sel })
                    selecting = false
                    sel = emptySet()
                }, active = sel.isNotEmpty(), enabled = sel.isNotEmpty())
                Btn("+playlist", {
                    addToPlaylist(list.filter { it.path in sel })
                    selecting = false
                    sel = emptySet()
                }, active = sel.isNotEmpty(), enabled = sel.isNotEmpty())
                Btn("cancelar", { selecting = false; sel = emptySet() })
            }
        }
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt("${list.size} faixas", color = p.dim, size = 11.sp, modifier = Modifier.padding(start = 6.dp).weight(1f))
            Btn("bloquear pasta", { blockAsk = true }, size = 11.sp)
        }
        Rule()
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(list, key = { _, t -> t.path }) { i, t ->
                TrackRow(
                    label = (i + 1).toString().padStart(2, '0'),
                    track = t,
                    playing = st.path == t.path,
                    selectMode = selecting,
                    selected = t.path in sel,
                    onClick = {
                        if (selecting) sel = if (t.path in sel) sel - t.path else sel + t.path
                        else vm.playTracks(list, i)
                    },
                    menu = listOf(
                        "adicionar à playlist" to { addToPlaylist(listOf(t)) },
                        "editar etiquetas" to { openTags(t) },
                    ),
                    onArtistClick = openArtist,
                )
            }
        }
    }

    if (blockAsk) {
        ConfirmDialog(
            title = "bloquear pasta",
            body = "${Library.shortPath(folder)}\n\nas faixas desta pasta deixam de aparecer na biblioteca.",
            okLabel = "bloquear",
            onOk = { vm.block(folder); blockAsk = false; back() },
            onDismiss = { blockAsk = false },
        )
    }
}

// ------------------------------------------------------------------ playlists

@Composable
fun PlaylistsScreen(vm: AppViewModel, open: (String) -> Unit) {
    val p = LocalPal.current
    val pls by vm.playlists.collectAsState()
    var creating by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Prompt("playlists") { Btn("+ nova", { creating = true }) }
        Rule()
        if (pls.isEmpty()) {
            Txt(
                "> sem playlists. cria uma aqui, ou escolhe faixas numa pasta (selecionar > +playlist).",
                color = p.dim, modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(pls, key = { it.id }) { pl ->
                Column(Modifier.fillMaxWidth().clickable { open(pl.id) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Txt(pl.name, color = p.accent, bold = true, maxLines = 1)
                    Txt(
                        "${pl.paths.size} faixas · aleat:${if (pl.shuffle) "on" else "off"} · rep:${Rep.label(pl.repeat)}",
                        color = p.dim, size = 11.sp,
                    )
                }
                Rule()
            }
        }
    }

    if (creating) {
        InputDialog(
            "nova playlist", okLabel = "criar",
            onOk = { vm.createPlaylist(it); creating = false },
            onDismiss = { creating = false },
        )
    }
}

@Composable
fun PlaylistScreen(vm: AppViewModel, id: String, back: () -> Unit, openTags: (Track) -> Unit, openArtist: (String) -> Unit = {}) {
    val p = LocalPal.current
    val pls by vm.playlists.collectAsState()
    val byPath by vm.allByPath.collectAsState()
    val st by vm.player.state.collectAsState()
    val pl = pls.firstOrNull { it.id == id }
    if (pl == null) {
        LaunchedEffect(Unit) { back() }
        return
    }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Prompt(pl.name) { Btn("voltar", back) }
        Flow(Modifier.padding(horizontal = 10.dp)) {
            Btn("tocar", { vm.playPlaylist(pl) }, active = true, enabled = pl.paths.isNotEmpty())
            Btn("aleat:${if (pl.shuffle) "on" else "off"}", { vm.setPlaylistShuffle(pl.id, !pl.shuffle) }, active = pl.shuffle)
            Btn("rep:${Rep.label(pl.repeat)}", { vm.cyclePlaylistRepeat(pl.id) }, active = pl.repeat != Rep.OFF)
            Btn("renomear", { renaming = true })
            Btn("apagar", { deleting = true })
        }
        Txt("${pl.paths.size} faixas", color = p.dim, size = 11.sp, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        Rule()
        if (pl.paths.isEmpty()) {
            Txt("> vazia. abre uma pasta na biblioteca e usa selecionar > +playlist.", color = p.dim, modifier = Modifier.padding(16.dp))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(pl.paths, key = { _, path -> path }) { i, path ->
                val t = byPath[path]
                if (t != null) {
                    TrackRow(
                        label = (i + 1).toString().padStart(2, '0'),
                        track = t,
                        playing = st.path == path,
                        onClick = { vm.playPlaylist(pl, startPath = path) },
                        menu = listOf(
                            "remover da playlist" to { vm.removeFromPlaylist(pl.id, path) },
                            "editar etiquetas" to { openTags(t) },
                        ),
                        onArtistClick = openArtist,
                        showCover = true,
                    )
                } else {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Txt("(em falta) ${path.substringAfterLast('/')}", color = p.dim, size = 11.sp, maxLines = 1, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
                        Btn("-", { vm.removeFromPlaylist(pl.id, path) })
                    }
                }
            }
        }
    }

    if (renaming) {
        InputDialog("renomear", initial = pl.name, okLabel = "guardar",
            onOk = { vm.renamePlaylist(pl.id, it); renaming = false },
            onDismiss = { renaming = false })
    }
    if (deleting) {
        ConfirmDialog("apagar playlist", "\"${pl.name}\" será apagada (as músicas não são apagadas).", "apagar",
            onOk = { deleting = false; vm.deletePlaylist(pl.id) },
            onDismiss = { deleting = false })
    }
}

@Composable
fun AddToPlaylistDialog(vm: AppViewModel, tracks: List<Track>, onDone: () -> Unit) {
    val p = LocalPal.current
    val pls by vm.playlists.collectAsState()
    var name by remember { mutableStateOf("") }
    val paths = tracks.map { it.path }
    TermDialog("adicionar ${tracks.size} faixa(s)", onDone) {
        pls.forEach { pl ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.addToPlaylist(pl.id, paths); onDone() }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("+ ${pl.name}", color = p.accent, modifier = Modifier.weight(1f), maxLines = 1)
                Txt("${pl.paths.size}", color = p.dim, size = 11.sp)
            }
            Rule()
        }
        Spacer(Modifier.size(8.dp))
        TermField("nova", name, { name = it })
        Btn("criar e adicionar", {
            if (name.isNotBlank()) { vm.createPlaylist(name, paths); onDone() }
        }, enabled = name.isNotBlank())
    }
}

// ---------------------------------------------------------------- definições

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val p = LocalPal.current
    val accent by vm.accent.collectAsState()
    val language by vm.language.collectAsState()
    val blocked by vm.blocked.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Prompt("definições")
        Rule()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Section("Idioma")
            Flow {
                Btn("Português", { vm.setLanguage("pt") }, active = language == "pt")
                Btn("English", { vm.setLanguage("en") }, active = language == "en")
            }

            Section("paleta")
            Flow {
                Accent.entries.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(a.color))
                        Btn(a.label, { vm.setAccent(a) }, active = a == accent)
                    }
                }
            }

            MusicFoldersPicker(vm)

            Section("pastas bloqueadas")
            if (blocked.isEmpty()) {
                Txt("nenhuma. bloqueia pastas na biblioteca.", color = p.dim, size = 11.sp)
            }
            blocked.sorted().forEach { f ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Txt(Library.shortPath(f), modifier = Modifier.weight(1f), maxLines = 2)
                    Btn("desbloquear", { vm.unblock(f) }, size = 11.sp)
                }
                Rule()
            }

            Section("sobre")
            Txt("miglycal v${BuildConfig.VERSION_NAME}", color = p.dim)
            Txt("flac // mp3", color = p.dim, size = 11.sp)
        }
    }
}
