package app.miglycal.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miglycal.AppViewModel
import app.miglycal.data.Library

@Composable
fun FirstRunSetupScreen(vm: AppViewModel) {
    val p = LocalPal.current
    val folders by vm.musicFolders.collectAsState()
    val accent by vm.accent.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.size(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("miglycal", size = 30.sp, bold = true, color = p.accent)
            BlinkCursor(30.sp)
        }
        Txt("Set up your music library", color = p.dim, size = 13.sp)
        Spacer(Modifier.size(12.dp))
        Txt("Choose the folders where your music is stored and pick an accent colour. You can change these later in Settings.", size = 12.sp)
        MusicFoldersPicker(vm)
        Section("accent colour")
        Flow {
            Accent.entries.forEach { choice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(10.dp).background(choice.color)
                    )
                    Btn(choice.label, { vm.setAccent(choice) }, active = choice == accent)
                }
            }
        }
        Spacer(Modifier.size(20.dp))
        Btn(
            "start using miglycal",
            { vm.completeSetup() },
            active = true,
            enabled = folders.isNotEmpty(),
            size = 14.sp,
        )
        if (folders.isEmpty()) {
            Txt("> choose at least one music folder to continue.", color = p.dim, size = 11.sp)
        }
        Spacer(Modifier.size(28.dp))
    }
}

/** Opens Android's system folder picker; only the selected folder is used. */
@Composable
fun MusicFoldersPicker(vm: AppViewModel) {
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

    Section("music folders")
    Txt("Only audio files inside these folders will appear in your library. Android opens its own folder picker; no broad storage permission is needed for selecting a folder.", color = p.dim, size = 11.sp)
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
