package app.miglycal.ui

import android.app.Activity
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miglycal.AppViewModel
import app.miglycal.data.AlbumInfo
import app.miglycal.data.AlbumOnline
import app.miglycal.data.AlbumTagField
import app.miglycal.data.AlbumTrackInfo
import app.miglycal.data.Art
import app.miglycal.data.ArtistNames
import app.miglycal.data.TagIO
import app.miglycal.data.Tags
import app.miglycal.data.Track
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

private data class AlbumTrackRow(val track: Track, val tags: Tags)

@Composable
fun AlbumScreen(
    album: String,
    artist: String,
    candidates: List<Track>,
    vm: AppViewModel,
    back: () -> Unit,
    editTags: (List<Track>) -> Unit,
) {
    val p = LocalPal.current
    val player by vm.player.state.collectAsState()
    var rows by remember(album, artist) { mutableStateOf<List<AlbumTrackRow>>(emptyList()) }
    var info by remember(album, artist) { mutableStateOf<AlbumInfo?>(null) }
    var loading by remember(album, artist) { mutableStateOf(true) }
    var localCoverAvailable by remember(album, artist) { mutableStateOf(false) }
    var expandedDescription by remember(album, artist) { mutableStateOf(false) }

    LaunchedEffect(album, artist, candidates) {
        loading = true
        val read = withContext(Dispatchers.IO) {
            candidates.map { AlbumTrackRow(it, TagIO.read(it.path)) }
        }
        val matching = read.filter { row ->
            val albumArtist = row.tags.albumArtist
            val identity = albumArtist.ifBlank { row.track.artist }
            ArtistNames.split(identity).any { it.equals(artist, ignoreCase = true) } ||
                identity.equals(artist, ignoreCase = true)
        }
        val selected = (matching.ifEmpty { read }).sortedWith(
            compareBy<AlbumTrackRow> { it.tags.disc.substringBefore('/').toIntOrNull() ?: 1 }
                .thenBy { it.tags.track.substringBefore('/').toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { it.track.title.lowercase() }
        )
        rows = selected
        info = runCatching { AlbumOnline.info(artist, album) }.getOrNull()
        loading = false
    }

    LaunchedEffect(rows) {
        val path = rows.firstOrNull()?.track?.path
        localCoverAvailable = path != null && withContext(Dispatchers.IO) { Art.load(path) != null }
    }

    val localTracks = rows.map { it.track }
    val commonTags = rows.map { it.tags }
    val localYear = commonTags.map { it.year }.firstOrNull { it.isNotBlank() }
    val localGenre = commonTags.map { it.genre }.firstOrNull { it.isNotBlank() }
    val year = localYear ?: info?.releasedAt?.let { Regex("\\b(?:19|20)\\d{2}\\b").find(it)?.value }
    val genre = localGenre ?: info?.genres?.take(3)?.joinToString(" · ").orEmpty()

    Column(Modifier.fillMaxSize()) {
        Prompt(album.ifBlank { "álbum desconhecido" }) { Btn("voltar", back) }
        Rule()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Btn("tocar", { vm.playTracks(localTracks, 0) }, active = true, enabled = localTracks.isNotEmpty())
            Btn("aleat.", { vm.playShuffled(localTracks) }, enabled = localTracks.isNotEmpty())
            Spacer(Modifier.weight(1f))
            Btn("editar etiquetas", { if (localTracks.isNotEmpty()) editTags(localTracks) }, enabled = localTracks.isNotEmpty())
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(Modifier.size(132.dp).background(p.surface)) {
                    val localCover = rows.firstOrNull()?.track?.path
                    if (localCover != null && localCoverAvailable) {
                        CoverImage(localCover, Modifier.fillMaxSize())
                    } else if (info?.imageUrl != null) {
                        AsyncImage(
                            model = info?.imageUrl,
                            contentDescription = "Album cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else if (localCover != null) {
                        CoverImage(localCover, Modifier.fillMaxSize())
                    }
                }
                Column(Modifier.weight(1f)) {
                    Txt(album, size = 19.sp, bold = true, color = p.accent, maxLines = 3)
                    Txt(artist, color = p.text, size = 13.sp, maxLines = 2)
                    if (!year.isNullOrBlank()) Txt(year, color = p.dim, size = 11.sp)
                    if (genre.isNotBlank()) Txt(genre, color = p.dim, size = 11.sp, maxLines = 3)
                    Txt("${localTracks.size} faixa(s)", color = p.dim, size = 11.sp)
                }
            }

            val description = info?.description.orEmpty()
            if (description.isNotBlank()) {
                Section("descrição")
                Txt(
                    description,
                    size = 12.sp,
                    maxLines = if (expandedDescription) Int.MAX_VALUE else 4,
                )
                if (description.length > 180) {
                    Btn(
                        if (expandedDescription) "mostrar menos" else "ler mais",
                        { expandedDescription = !expandedDescription },
                    )
                }
            } else if (loading) {
                Section("last.fm")
                Txt("> a carregar informações do álbum...", color = p.dim, size = 11.sp)
            }

            Section("tracklist")
            if (rows.isEmpty()) {
                Txt("> não há faixas locais associadas a este álbum.", color = p.dim, size = 11.sp)
                info?.tracks?.forEach { item -> AlbumRemoteTrack(item) }
            } else {
                rows.forEachIndexed { index, row ->
                    TrackRow(
                        label = row.tags.track.substringBefore('/').takeIf { it.isNotBlank() }
                            ?: (index + 1).toString().padStart(2, '0'),
                        track = row.track,
                        playing = player.path == row.track.path,
                        onClick = { vm.playTracks(localTracks, index) },
                        showCover = false,
                    )
                    Rule()
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AlbumRemoteTrack(track: AlbumTrackInfo) {
    val p = LocalPal.current
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Txt(track.position.toString().padStart(2, '0'), color = p.dim, modifier = Modifier.width(34.dp))
        Txt(track.name, modifier = Modifier.weight(1f), maxLines = 2)
        if (track.durationSeconds > 0) {
            val minutes = track.durationSeconds / 60
            val seconds = track.durationSeconds % 60
            Txt(String.format("%d:%02d", minutes, seconds), color = p.dim, size = 11.sp)
        }
    }
    Rule()
}

@Composable
fun AlbumTagEditorScreen(
    vm: AppViewModel,
    album: String,
    tracks: List<Track>,
    back: () -> Unit,
) {
    val p = LocalPal.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var values by remember(album, tracks) {
        mutableStateOf(Tags(album = album))
    }
    var selectedFields by remember(album, tracks) { mutableStateOf(setOf<AlbumTagField>()) }
    var mixedFields by remember(album, tracks) { mutableStateOf(setOf<AlbumTagField>()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(album, tracks) {
        val allTags = withContext(Dispatchers.IO) { tracks.map { TagIO.read(it.path) } }
        fun common(selector: (Tags) -> String): String {
            val entries = allTags.map(selector).distinct()
            return entries.singleOrNull().orEmpty()
        }
        val mixed = buildSet {
            if (allTags.map { it.album }.distinct().size > 1) add(AlbumTagField.ALBUM)
            if (allTags.map { it.albumArtist }.distinct().size > 1) add(AlbumTagField.ALBUM_ARTIST)
            if (allTags.map { it.year }.distinct().size > 1) add(AlbumTagField.YEAR)
            if (allTags.map { it.genre }.distinct().size > 1) add(AlbumTagField.GENRE)
            if (allTags.map { it.comment }.distinct().size > 1) add(AlbumTagField.COMMENT)
        }
        mixedFields = mixed
        values = Tags(
            album = common { it.album }.ifBlank { album },
            albumArtist = common { it.albumArtist },
            year = common { it.year },
            genre = common { it.genre },
            comment = common { it.comment },
        )
        selectedFields = buildSet {
            if (values.album.isNotBlank() && AlbumTagField.ALBUM !in mixed) add(AlbumTagField.ALBUM)
            if (values.albumArtist.isNotBlank() && AlbumTagField.ALBUM_ARTIST !in mixed) add(AlbumTagField.ALBUM_ARTIST)
            if (values.year.isNotBlank() && AlbumTagField.YEAR !in mixed) add(AlbumTagField.YEAR)
            if (values.genre.isNotBlank() && AlbumTagField.GENRE !in mixed) add(AlbumTagField.GENRE)
            if (values.comment.isNotBlank() && AlbumTagField.COMMENT !in mixed) add(AlbumTagField.COMMENT)
        }
    }

    fun toggle(field: AlbumTagField) {
        selectedFields = if (field in selectedFields) selectedFields - field else selectedFields + field
    }

    fun save() {
        if (selectedFields.isEmpty()) {
            status = "! seleciona pelo menos um campo para aplicar."
            return
        }
        val tagValues = values
        val fieldsToApply = selectedFields
        scope.launch {
            busy = true
            status = "> a guardar etiquetas em ${tracks.size} faixa(s)..."
            val results = withContext(Dispatchers.IO) {
                tracks.map { track ->
                    track.path to runCatching {
                        if (Build.VERSION.SDK_INT >= 30) {
                            TagIO.writeAlbumViaUri(context, Uri.parse(track.uri), tagValues, fieldsToApply)
                        } else {
                            TagIO.writeAlbum(context, track.path, tagValues, fieldsToApply)
                        }
                    }
                }
            }
            val failed = results.filter { it.second.isFailure }
            val paths = tracks.map { it.path }.toTypedArray()
            if (paths.isNotEmpty()) {
                val remaining = AtomicInteger(paths.size)
                MediaScannerConnection.scanFile(context, paths, null) { _, _ ->
                    if (remaining.decrementAndGet() == 0) vm.refresh()
                }
            } else {
                vm.refresh()
            }
            status = if (failed.isEmpty()) {
                "> etiquetas guardadas em ${results.size} faixa(s)."
            } else {
                "! ${failed.size} de ${results.size} falharam: ${failed.first().second.exceptionOrNull()?.message.orEmpty()}"
            }
            busy = false
            if (failed.isEmpty()) back()
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) save() else status = "! permissão recusada; nada foi alterado."
    }

    Column(Modifier.fillMaxSize()) {
        Prompt("etiquetas do álbum") { Btn("voltar", back) }
        Rule()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Txt(album, color = p.accent, bold = true, modifier = Modifier.padding(vertical = 8.dp))
            Txt("As alterações selecionadas são aplicadas apenas aos campos assinalados. Título e artista de cada faixa ficam intactos.", color = p.dim, size = 11.sp)
            Section("campos a aplicar")

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Btn(if (AlbumTagField.ALBUM in selectedFields) "✓ álbum" else "álbum", { toggle(AlbumTagField.ALBUM) }, active = AlbumTagField.ALBUM in selectedFields)
                if (AlbumTagField.ALBUM in mixedFields) Txt("valores diferentes", color = p.dim, size = 10.sp)
            }
            TermField("álbum", values.album, { values = values.copy(album = it) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Btn(if (AlbumTagField.ALBUM_ARTIST in selectedFields) "✓ artista álb." else "artista álb.", { toggle(AlbumTagField.ALBUM_ARTIST) }, active = AlbumTagField.ALBUM_ARTIST in selectedFields)
                if (AlbumTagField.ALBUM_ARTIST in mixedFields) Txt("valores diferentes", color = p.dim, size = 10.sp)
            }
            TermField("artista álb.", values.albumArtist, { values = values.copy(albumArtist = it) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Btn(if (AlbumTagField.YEAR in selectedFields) "✓ ano" else "ano", { toggle(AlbumTagField.YEAR) }, active = AlbumTagField.YEAR in selectedFields)
                if (AlbumTagField.YEAR in mixedFields) Txt("valores diferentes", color = p.dim, size = 10.sp)
            }
            TermField("ano", values.year, { values = values.copy(year = it) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Btn(if (AlbumTagField.GENRE in selectedFields) "✓ género" else "género", { toggle(AlbumTagField.GENRE) }, active = AlbumTagField.GENRE in selectedFields)
                if (AlbumTagField.GENRE in mixedFields) Txt("valores diferentes", color = p.dim, size = 10.sp)
            }
            TermField("género", values.genre, { values = values.copy(genre = it) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Btn(if (AlbumTagField.COMMENT in selectedFields) "✓ comentário" else "comentário", { toggle(AlbumTagField.COMMENT) }, active = AlbumTagField.COMMENT in selectedFields)
                if (AlbumTagField.COMMENT in mixedFields) Txt("valores diferentes", color = p.dim, size = 10.sp)
            }
            TermField("comentário", values.comment, { values = values.copy(comment = it) }, singleLine = false)
            Section("aplicação")
            Txt("${tracks.size} faixa(s) serão processadas. O Miglycal pede autorização do Android para modificar os ficheiros.", color = p.dim, size = 11.sp)
            if (status.isNotBlank()) Txt(status, color = if (status.startsWith("!")) p.text else p.dim, size = 11.sp)
            Spacer(Modifier.height(24.dp))
        }
        Rule()
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Btn("guardar em todas", {
                if (!busy && selectedFields.isNotEmpty()) {
                    if (Build.VERSION.SDK_INT >= 30) {
                        val request = MediaStore.createWriteRequest(
                            context.contentResolver,
                            tracks.map { Uri.parse(it.uri) },
                        )
                        permission.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    } else save()
                }
            }, active = true, enabled = !busy && tracks.isNotEmpty() && selectedFields.isNotEmpty())
            Btn("cancelar", back, enabled = !busy)
        }
    }
}
