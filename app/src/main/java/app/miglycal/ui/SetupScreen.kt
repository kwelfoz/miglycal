package app.miglycal.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miglycal.AppViewModel
import app.miglycal.R
import app.miglycal.data.Library

@Composable
fun FirstRunSetupScreen(vm: AppViewModel) {
    val p = LocalPal.current
    val folders by vm.musicFolders.collectAsState()
    val accent by vm.accent.collectAsState()
    val language by vm.language.collectAsState()
    var showSetup by rememberSaveable { mutableStateOf(false) }

    if (!showSetup) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(156.dp).background(androidx.compose.ui.graphics.Color(0xFF101010))
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.miglycal_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.size(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("miglycal", size = 30.sp, bold = true, color = p.accent)
                BlinkCursor(30.sp)
            }
            Spacer(Modifier.size(10.dp))
            Txt(
                "Um leitor de música para a tua coleção local.",
                color = p.dim,
                size = 13.sp,
                align = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(30.dp))
            Btn("Começar agora", { showSetup = true }, active = true, size = 15.sp)
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.size(14.dp))
            Prompt("Configuração inicial")
            Txt(
                "Escolhe a língua, a cor e as pastas onde guardas a tua música.",
                color = p.dim,
                size = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SetupCard("Idioma") {
                Flow {
                    Btn("Português", { vm.setLanguage("pt") }, active = language == "pt")
                    Btn("English", { vm.setLanguage("en") }, active = language == "en")
                }
            }

            SetupCard("Cor de destaque") {
                Flow {
                    Accent.entries.forEach { choice ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(choice.color))
                            Btn(choice.label, { vm.setAccent(choice) }, active = choice == accent)
                        }
                    }
                }
            }

            SetupCard("Pastas de música") {
                MusicFoldersPicker(vm, showTitle = false)
            }

            Spacer(Modifier.size(12.dp))
            Btn(
                "Começar a usar o Miglycal",
                { vm.completeSetup() },
                active = true,
                enabled = folders.isNotEmpty(),
                size = 14.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            if (folders.isEmpty()) {
                Txt(
                    "> Escolhe pelo menos uma pasta de música para continuar.",
                    color = p.dim,
                    size = 11.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.size(28.dp))
        }
    }
}

@Composable
private fun SetupCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPal.current
    Column(
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .background(p.surface)
            .border(1.dp, p.line)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Section(title, Modifier.padding(top = 0.dp))
        content()
    }
}

/** Opens Android's system folder picker; only the selected folder is used. */
@Composable
fun MusicFoldersPicker(vm: AppViewModel, showTitle: Boolean = true) {
    val p = LocalPal.current
    val folders by vm.musicFolders.collectAsState()
    var pickerError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val path = Library.pathFromTreeUri(uri)
        if (path == null) {
            pickerError = "This folder location is not supported. Choose a folder in shared internal storage or an SD card."
        } else {
            pickerError = null
            vm.addMusicFolder(path)
        }
    }

    if (showTitle) Section("music folders")
    Txt(
        "Only audio files inside these folders will appear in your library. Android opens its own folder picker; no broad storage permission is needed for selecting a folder.",
        color = p.dim,
        size = 11.sp,
    )
    Spacer(Modifier.size(5.dp))
    Btn("add folder", { picker.launch(null) }, active = true)
    if (folders.isEmpty()) {
        Txt("> no folders selected.", color = p.dim, size = 11.sp)
    } else {
        folders.sorted().forEach { path ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt(Library.shortPath(path), color = p.text, size = 11.sp, maxLines = 2)
                }
                Btn(
                    "remove",
                    { vm.removeMusicFolder(path) },
                    enabled = folders.size > 1,
                    size = 10.sp,
                )
            }
            Rule()
        }
        if (folders.size == 1) {
            Txt("Keep at least one folder selected.", color = p.dim, size = 10.sp)
        }
    }
    pickerError?.let { Txt("> $it", color = p.text, size = 11.sp) }
}
