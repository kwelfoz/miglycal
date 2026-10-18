package app.miglycal.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miglycal.AppViewModel
import app.miglycal.data.ArtistInfo
import app.miglycal.data.ArtistNames
import app.miglycal.data.ArtistOnline
import app.miglycal.data.Track
import coil.compose.AsyncImage

@Composable
fun ArtistScreen(name: String, vm: AppViewModel, back: () -> Unit) {
    val p = LocalPal.current
    val tracks by vm.tracks.collectAsState()
    val player by vm.player.state.collectAsState()
    var info by remember(name) { mutableStateOf<ArtistInfo?>(null) }
    var loading by remember(name) { mutableStateOf(true) }
    var expandedBio by remember(name) { mutableStateOf(false) }

    val localTracks = remember(tracks, name) {
        tracks.filter { track ->
            ArtistNames.split(track.artist).any { it.equals(name, ignoreCase = true) }
        }.sortedWith(compareBy<Track> { it.album.lowercase() }.thenBy { it.title.lowercase() })
    }
    val albums = remember(localTracks) { localTracks.groupBy { it.album.ifBlank { "álbum desconhecido" } } }

    LaunchedEffect(name) {
        loading = true
        info = runCatching { ArtistOnline.info(name) }.getOrNull()
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        Prompt(name) { Btn("voltar", back) }
        Rule()
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            if (info?.imageUrl != null) {
                AsyncImage(
                    model = info?.imageUrl,
                    contentDescription = "Artist photo",
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(12.dp))
            }
            Txt(info?.name ?: name, color = p.accent, size = 21.sp, bold = true)
            val bio = info?.bio.orEmpty()
            if (bio.isNotBlank()) {
                Section("about")
                Txt(bio, size = 12.sp, maxLines = if (expandedBio) Int.MAX_VALUE else 4)
                if (bio.length > 180) {
                    Btn(if (expandedBio) "show less" else "read more", { expandedBio = !expandedBio })
                }
            } else if (loading) {
                Txt("> loading artist information...", color = p.dim, size = 11.sp)
            } else {
                Txt("> no biography available.", color = p.dim, size = 11.sp)
            }

            Section("in your library")
            Txt(
                "${localTracks.size} track(s) · ${albums.size} album(s)",
                color = p.dim, size = 11.sp,
            )
            if (localTracks.isEmpty()) {
                Txt("> no music by this artist was found in your selected folders.", color = p.dim, size = 11.sp)
            } else {
                albums.forEach { (album, albumTracks) ->
                    Section(album)
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                            CoverImage(albumTracks.firstOrNull()?.path, Modifier.fillMaxSize())
                        }
                        Column(Modifier.weight(1f)) {
                            Txt(album, color = p.text, bold = true, maxLines = 2)
                            Txt("${albumTracks.size} track(s)", color = p.dim, size = 10.sp)
                        }
                        Btn("play", { vm.playTracks(albumTracks, 0) }, active = true, size = 10.sp)
                    }
                    albumTracks.forEachIndexed { index, track ->
                        TrackRow(
                            label = (index + 1).toString().padStart(2, '0'),
                            track = track,
                            playing = player.path == track.path,
                            onClick = { vm.playTracks(albumTracks, index) },
                        )
                        Rule()
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
