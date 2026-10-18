package app.miglycal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.miglycal.data.Art
import app.miglycal.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format("%d:%02d", s / 60, s % 60)
}

@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPal.current.text,
    size: TextUnit = 13.sp,
    bold: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
) {
    Text(
        text = localizeText(text, LocalLanguage.current),
        modifier = modifier,
        color = color,
        fontSize = size,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        lineHeight = size * 1.4f,
    )
}

/** Botão de texto: [assim]. Quando ativo fica com o fundo no acento. */
@Composable
fun Btn(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    size: TextUnit = 13.sp,
) {
    val p = LocalPal.current
    val fg = when {
        !enabled -> p.dim.copy(alpha = 0.5f)
        active -> p.bg
        else -> p.accent
    }
    Box(
        modifier
            .background(if (active) p.accent else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt("[$label]", color = fg, size = size, bold = active)
    }
}

@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalPal.current.line))
}

@Composable
fun BlinkCursor(size: TextUnit = 16.sp) {
    var on by remember { mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            delay(530)
            on = !on
        }
    }
    Txt("_", color = if (on) LocalPal.current.accent else Color.Transparent, size = size, bold = true)
}

@Composable
fun Prompt(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    val p = LocalPal.current
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt("> ", color = p.accent, size = 16.sp, bold = true)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Txt(title, size = 16.sp, bold = true, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
            BlinkCursor()
        }
        trailing()
    }
}

@Composable
fun Section(title: String, modifier: Modifier = Modifier) {
    Txt("# $title", color = LocalPal.current.accent, bold = true, modifier = modifier.padding(top = 18.dp, bottom = 6.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Flow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(modifier, verticalArrangement = Arrangement.Center) { content() }
}

/** Campo de texto estilo terminal:  rótulo: valor_ */
@Composable
fun TermField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val p = LocalPal.current
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Txt("$label:", color = p.accent, modifier = Modifier.width(112.dp).padding(top = 2.dp), maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = singleLine,
                textStyle = TextStyle(color = p.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                cursorBrush = SolidColor(p.accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f).padding(vertical = 2.dp),
            )
        }
        Rule()
    }
}

@Composable
fun TermDialog(
    title: String,
    onDismiss: () -> Unit,
    confirm: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPal.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = p.surfaceHi,
        title = { Txt("> $title", color = p.accent, bold = true, size = 15.sp) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), content = content) },
        confirmButton = { confirm?.invoke() },
        dismissButton = { Btn("cancelar", onDismiss) },
    )
}

@Composable
fun InputDialog(
    title: String,
    initial: String = "",
    okLabel: String = "ok",
    onOk: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var v by remember { mutableStateOf(initial) }
    TermDialog(
        title, onDismiss,
        confirm = { Btn(okLabel, { if (v.isNotBlank()) onOk(v.trim()) }, active = true) },
    ) {
        TermField("nome", v, { v = it })
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, okLabel: String, onOk: () -> Unit, onDismiss: () -> Unit) {
    TermDialog(title, onDismiss, confirm = { Btn(okLabel, onOk, active = true) }) {
        Txt(body, size = 12.sp)
    }
}

/** Linha de faixa. `label` é o prefixo (nº da faixa); `menu` são ações extra. */
@Composable
fun TrackRow(
    label: String,
    track: Track,
    playing: Boolean,
    onClick: () -> Unit,
    selectMode: Boolean = false,
    selected: Boolean = false,
    menu: List<Pair<String, () -> Unit>> = emptyList(),
    onArtistClick: ((String) -> Unit)? = null,
    showCover: Boolean = false,
) {
    val p = LocalPal.current
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (playing) p.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val prefix = when {
            selectMode -> if (selected) "[x]" else "[ ]"
            playing -> " > "
            else -> label
        }
        Txt(prefix, color = if (playing || selected) p.accent else p.dim, modifier = Modifier.width(34.dp), maxLines = 1)
        if (showCover) {
            CoverImage(track.path, Modifier.size(42.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Txt(track.title, color = if (playing) p.accent else p.text, bold = playing, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val artists = app.miglycal.data.ArtistNames.split(track.artist)
                artists.forEachIndexed { index, artist ->
                    if (index > 0) Txt(", ", color = p.dim, size = 11.sp)
                    Txt(
                        artist,
                        color = if (onArtistClick != null) p.accent else p.dim,
                        size = 11.sp,
                        maxLines = 1,
                        modifier = if (onArtistClick != null) Modifier.clickable { onArtistClick(artist) } else Modifier,
                    )
                }
                Txt(" · ${track.album}", color = p.dim, size = 11.sp, maxLines = 1)
            }
        }
        Txt(fmt(track.durationMs), color = p.dim, size = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
        if (menu.isNotEmpty()) {
            Box {
                Btn("..", { open = true })
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    menu.forEach { (name, act) ->
                        DropdownMenuItem(text = { Txt(name) }, onClick = { open = false; act() })
                    }
                }
            }
        }
    }
}

/** Capa (embebida no ficheiro). Mostra "no cover" se não existir. */
@Composable
fun CoverImage(path: String?, modifier: Modifier = Modifier, version: Int = 0) {
    val p = LocalPal.current
    val bmp by produceState<android.graphics.Bitmap?>(null, path, version) {
        value = if (path == null) null else withContext(Dispatchers.IO) { Art.load(path) }
    }
    Box(modifier.background(p.surface).border(1.dp, p.line), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Txt("no cover", color = p.dim, size = 11.sp)
        }
    }
}

/** Barra de progresso em blocos, com toque e arrastar para procurar. */
@Composable
fun TermBar(frac: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier, segments: Int = 36) {
    val p = LocalPal.current
    var drag by remember { mutableStateOf<Float?>(null) }
    val seek by rememberUpdatedState(onSeek)
    val shown = (drag ?: frac).coerceIn(0f, 1f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { seek((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = (it.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { drag?.let(seek); drag = null },
                    onDragCancel = { drag = null },
                    onHorizontalDrag = { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) },
                )
            }
    ) {
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (segments - 1)) / segments
        val filled = (shown * segments).toInt()
        for (i in 0 until segments) {
            drawRect(
                color = if (i < filled) p.accent else p.line,
                topLeft = Offset(i * (w + gap), size.height * 0.25f),
                size = Size(w, size.height * 0.5f),
            )
        }
    }
}

@Composable
fun NavBar(current: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    Column(modifier.background(p.bg)) {
        Rule()
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("biblioteca", "playlists", "definições").forEach {
                Btn(it, { onSelect(it) }, active = it == current)
            }
        }
    }
}
