package app.miglycal.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miglycal.data.ArtistInfo
import app.miglycal.data.ArtistOnline
import app.miglycal.data.ArtistRelease
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun ArtistScreen(name: String, apiKey: String, back: () -> Unit) {
    val p = LocalPal.current
    val scope = rememberCoroutineScope()
    var info by remember(name, apiKey) { mutableStateOf<ArtistInfo?>(null) }
    var releases by remember(name) { mutableStateOf<List<ArtistRelease>>(emptyList()) }
    var loading by remember(name, apiKey) { mutableStateOf(true) }
    var error by remember(name, apiKey) { mutableStateOf<String?>(null) }

    LaunchedEffect(name, apiKey) {
        loading = true
        error = null
        val releaseJob = scope.launch {
            releases = runCatching { ArtistOnline.releases(name) }.getOrDefault(emptyList())
        }
        if (apiKey.isNotBlank()) {
            runCatching { ArtistOnline.info(name, apiKey) }
                .onSuccess { info = it }
                .onFailure { error = it.message }
        } else {
            info = null
        }
        releaseJob.join()
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        Prompt(name) { Btn("voltar", back) }
        Rule()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            if (info?.imageUrl != null) {
                AsyncImage(
                    model = info?.imageUrl,
                    contentDescription = "Fotografia de $name",
                    modifier = Modifier.fillMaxWidth().height(210.dp),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(12.dp))
            }
            Txt(info?.name ?: name, color = p.accent, size = 21.sp, bold = true)
            if (apiKey.isBlank()) {
                Txt("Para carregar a foto e a biografia, adiciona a tua chave de API do Last.fm em Definições → Last.fm.", color = p.dim, size = 11.sp)
            }
            if (!info?.bio.isNullOrBlank()) {
                Section("biografia")
                Txt(info?.bio.orEmpty(), size = 12.sp)
            } else if (apiKey.isNotBlank() && !loading) {
                Txt(error?.let { "> Last.fm: $it" } ?: "> biografia não encontrada no Last.fm.", color = p.dim, size = 11.sp)
            }
            Section("álbuns")
            val albums = releases.filter { it.type == "ÁLBUM" }
            if (albums.isEmpty() && loading) Txt("> a procurar lançamentos...", color = p.dim, size = 11.sp)
            else if (albums.isEmpty()) Txt("> sem álbuns encontrados.", color = p.dim, size = 11.sp)
            albums.forEach { ReleaseRow(it) }
            Section("singles")
            val singles = releases.filter { it.type == "SINGLE" }
            if (singles.isEmpty() && !loading) Txt("> sem singles encontrados.", color = p.dim, size = 11.sp)
            singles.forEach { ReleaseRow(it) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReleaseRow(release: ArtistRelease) {
    val p = LocalPal.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (release.coverUrl != null) {
            AsyncImage(
                model = release.coverUrl,
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) { Txt("no art", color = p.dim, size = 9.sp) }
        }
        Column(Modifier.weight(1f)) {
            Txt(release.title, size = 12.sp, bold = true, maxLines = 2)
            Txt(release.year.ifBlank { "ano desconhecido" }, color = p.dim, size = 10.sp)
        }
        if (release.url != null) Btn("web", { /* release link intentionally informational only */ }, size = 10.sp)
    }
    Rule()
}
