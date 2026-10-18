package app.miglycal.ui

import android.media.MediaScannerConnection
import android.os.Build
import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miglycal.AppViewModel
import app.miglycal.data.Art
import app.miglycal.data.CoverCandidate
import app.miglycal.data.Lyrics
import app.miglycal.data.LyricsStore
import app.miglycal.data.Online
import app.miglycal.data.TagIO
import app.miglycal.data.Tags
import app.miglycal.data.Track
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TagEditorScreen(vm: AppViewModel, track: Track, back: () -> Unit) {
    val p = LocalPal.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val fallback = Tags(
        title = track.title,
        artist = track.artist.takeUnless { it == "desconhecido" }.orEmpty(),
        album = track.album.takeUnless { it == "desconhecido" }.orEmpty(),
    )
    var tags by remember(track.path) { mutableStateOf(fallback) }
    LaunchedEffect(track.path) {
        val read = withContext(Dispatchers.IO) { TagIO.read(track.path) }
        tags = if (read.isEmpty()) fallback else read
    }

    val log = remember { mutableStateListOf<String>() }
    var busy by remember { mutableStateOf(false) }
    var coverVersion by remember { mutableIntStateOf(0) }

    // capas
    var candidates by remember { mutableStateOf<List<CoverCandidate>>(emptyList()) }
    var cover by remember { mutableStateOf<ByteArray?>(null) }
    val stagedBmp = remember(cover) { cover?.let { Art.decode(it, 400) } }

    // letras
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var useSynced by remember { mutableStateOf(true) }

    fun say(s: String) { log.add(s) }

    fun doSave() {
        scope.launch {
            busy = true
            say("> a guardar...")
            val l = lyrics
            // letra sincronizada: grava o texto LRC na etiqueta; normal: grava o texto simples
            val embed: String? = if (l == null) null
            else if (useSynced && l.synced != null) l.synced
            else l.plain ?: l.synced?.let { LyricsStore.stripTimestamps(it) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= 30) {
                        TagIO.writeViaUri(ctx, Uri.parse(track.uri), tags, cover, embed)
                    } else {
                        TagIO.write(ctx, track.path, tags, cover, embed)
                    }
                }
            }
            result.onSuccess {
                if (cover != null) {
                    Art.invalidate(track.path)
                    coverVersion++
                    cover = null
                }
                say("> guardado.")
                MediaScannerConnection.scanFile(ctx, arrayOf(track.path), null) { _, _ -> vm.refresh() }
            }.onFailure {
                say("! erro ao guardar: ${it.message}")
            }
            busy = false
        }
    }

    // Android 11+: o sistema pede ao utilizador autorização para modificar ESTE ficheiro
    val grant = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) doSave() else say("! permissão recusada, nada foi guardado.")
    }

    Column(Modifier.fillMaxSize()) {
        Prompt("etiquetas") { Btn("voltar", back) }
        Rule()
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Txt(track.path.substringAfterLast('/'), color = p.dim, size = 11.sp, modifier = Modifier.padding(vertical = 6.dp))

            TermField("título", tags.title, { tags = tags.copy(title = it) })
            TermField("artista", tags.artist, { tags = tags.copy(artist = it) })
            TermField("álbum", tags.album, { tags = tags.copy(album = it) })
            TermField("artista álb.", tags.albumArtist, { tags = tags.copy(albumArtist = it) })
            TermField("ano", tags.year, { tags = tags.copy(year = it) }, keyboardType = KeyboardType.Number)
            TermField("faixa nº", tags.track, { tags = tags.copy(track = it) })
            TermField("disco nº", tags.disc, { tags = tags.copy(disc = it) })
            TermField("género", tags.genre, { tags = tags.copy(genre = it) })
            TermField("comentário", tags.comment, { tags = tags.copy(comment = it) }, singleLine = false)

            // ------------------------------------------------------------ capa
            Section("capa")
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Txt("atual", color = p.dim, size = 10.sp)
                    CoverImage(track.path, Modifier.size(96.dp), version = coverVersion)
                }
                Column {
                    Txt("nova", color = p.dim, size = 10.sp)
                    Box(Modifier.size(96.dp).background(p.surface).border(1.dp, p.line), contentAlignment = Alignment.Center) {
                        if (stagedBmp != null) {
                            Image(stagedBmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Txt("-", color = p.dim)
                        }
                    }
                }
            }
            Btn("procurar capa online", {
                scope.launch {
                    busy = true
                    val album = tags.album.ifBlank { tags.title }
                    say("> a procurar capas: ${tags.artist} - $album")
                    candidates = runCatching { Online.covers(tags.artist, album) }
                        .onFailure { say("! ${it.message}") }
                        .getOrDefault(emptyList())
                    say("> ${candidates.size} resultado(s). toca num para escolher.")
                    busy = false
                }
            }, enabled = !busy)
            candidates.forEach { c ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        scope.launch {
                            busy = true
                            say("> a descarregar capa...")
                            runCatching { Online.downloadCover(c) }
                                .onSuccess { cover = it; say("> capa pronta (${it.size / 1024} kB). guarda para aplicar.") }
                                .onFailure { say("! falhou: ${it.message}") }
                            busy = false
                        }
                    }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(model = c.previewUrl, contentDescription = null, modifier = Modifier.size(56.dp), contentScale = ContentScale.Crop)
                    Spacer(Modifier.size(10.dp))
                    Txt(c.label, size = 11.sp, maxLines = 3, modifier = Modifier.weight(1f))
                }
            }

            // ----------------------------------------------------------- letra
            Section("letra")
            Btn("procurar letra online", {
                scope.launch {
                    busy = true
                    say("> a procurar letra: ${tags.artist} - ${tags.title}")
                    val r = runCatching { Online.lyrics(tags.artist, tags.title, tags.album, track.durationMs) }
                        .onFailure { say("! ${it.message}") }
                        .getOrNull()
                    lyrics = r
                    useSynced = r?.synced != null
                    say(
                        when {
                            r == null -> "> não encontrada."
                            r.synced != null && r.plain != null -> "> encontrada: sincronizada e normal."
                            r.synced != null -> "> encontrada: sincronizada."
                            else -> "> encontrada: normal."
                        }
                    )
                    busy = false
                }
            }, enabled = !busy)
            lyrics?.let { l ->
                Row {
                    if (l.synced != null) Btn("sincronizada", { useSynced = true }, active = useSynced)
                    if (l.plain != null) Btn("normal", { useSynced = false }, active = !useSynced || l.synced == null)
                }
                val preview = if (useSynced && l.synced != null) l.synced else (l.plain ?: l.synced.orEmpty())
                Txt(preview.lines().take(8).joinToString("\n"), color = p.dim, size = 11.sp, modifier = Modifier.padding(top = 6.dp))
                Txt(
                    if (useSynced && l.synced != null) "ao guardar: grava a letra sincronizada (formato LRC) no campo de letra do ficheiro."
                    else "ao guardar: grava a letra normal no campo de letra do ficheiro.",
                    color = p.dim, size = 10.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }

            // ------------------------------------------------------------- log
            if (log.isNotEmpty()) {
                Section("log")
                log.takeLast(8).forEach { Txt(it, color = if (it.startsWith("!")) p.text else p.dim, size = 11.sp) }
            }
            Spacer(Modifier.size(24.dp))
        }
        Rule()
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Btn("guardar", {
                if (Build.VERSION.SDK_INT >= 30) {
                    val pi = MediaStore.createWriteRequest(ctx.contentResolver, listOf(Uri.parse(track.uri)))
                    grant.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } else {
                    doSave()
                }
            }, active = true, enabled = !busy)
            Btn("cancelar", back)
        }
    }
}
