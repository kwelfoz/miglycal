package app.miglycal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.miglycal.AppViewModel
import app.miglycal.data.Track

/** Ecrãs de detalhe empilhados por cima do separador atual. */
sealed interface Detail
data class DFolder(val path: String) : Detail
data class DPlaylist(val id: String) : Detail
data class DTags(val track: Track) : Detail

@Composable
fun MiglycalApp(vm: AppViewModel, hasPermission: Boolean, requestPermission: () -> Unit) {
    val accent by vm.accent.collectAsState()
    MiglycalTheme(accent) {
        val p = LocalPal.current
        Box(Modifier.fillMaxSize().background(p.bg)) {
            if (!hasPermission) {
                Box(Modifier.statusBarsPadding().navigationBarsPadding()) { PermissionScreen(requestPermission) }
            } else {
                MainShell(vm)
            }
        }
    }
}

@Composable
private fun MainShell(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf("biblioteca") }
    val stack = remember { mutableStateListOf<Detail>() }
    var full by remember { mutableStateOf(false) }
    var addTarget by remember { mutableStateOf<List<Track>?>(null) }
    val top = stack.lastOrNull()
    val focusMode = top is DTags // no editor de etiquetas esconde mini leitor e navegação

    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }

    BackHandler(enabled = full) { full = false }
    BackHandler(enabled = !full && stack.isNotEmpty()) { pop() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val d = top) {
                    is DFolder -> FolderScreen(
                        vm, d.path, back = { pop() },
                        openTags = { stack.add(DTags(it)) },
                        addToPlaylist = { addTarget = it },
                    )
                    is DPlaylist -> PlaylistScreen(
                        vm, d.id, back = { pop() },
                        openTags = { stack.add(DTags(it)) },
                    )
                    is DTags -> TagEditorScreen(vm, d.track) { pop() }
                    null -> when (tab) {
                        "biblioteca" -> LibraryScreen(
                            vm,
                            openFolder = { stack.add(DFolder(it)) },
                            openTags = { stack.add(DTags(it)) },
                            addToPlaylist = { addTarget = it },
                        )
                        "playlists" -> PlaylistsScreen(vm) { stack.add(DPlaylist(it)) }
                        else -> SettingsScreen(vm)
                    }
                }
            }
            if (!focusMode) {
                MiniPlayer(vm) { full = true }
                NavBar(tab, { tab = it; stack.clear() }, Modifier.navigationBarsPadding())
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }

        AnimatedVisibility(
            visible = full,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            FullPlayer(vm, onClose = { full = false }) { path ->
                vm.allByPath.value[path]?.let {
                    full = false
                    stack.add(DTags(it))
                }
            }
        }
    }

    addTarget?.let { AddToPlaylistDialog(vm, it) { addTarget = null } }
}
