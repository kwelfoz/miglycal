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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
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
    onAlbumClick: ((Track) -> Unit)? = null,
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
            }
            Txt(
                track.album,
                color = if (onAlbumClick != null) p.accent else p.dim,
                size = 11.sp,
                maxLines = 1,
                modifier = if (onAlbumClick != null) Modifier.clickable { onAlbumClick(track) } else Modifier,
            )
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


/** Interactive HSV colour wheel with exact HEX and RGB controls. */
@Composable
fun AccentColorPicker(color: Color, onColorChange: (Color) -> Unit) {
    val p = LocalPal.current
    var hex by remember { mutableStateOf("#55E67A") }
    var red by remember { mutableStateOf("85") }
    var green by remember { mutableStateOf("230") }
    var blue by remember { mutableStateOf("122") }
    LaunchedEffect(color) {
        val argb = color.toArgb()
        hex = String.format("#%02X%02X%02X", android.graphics.Color.red(argb), android.graphics.Color.green(argb), android.graphics.Color.blue(argb))
        red = android.graphics.Color.red(argb).toString()
        green = android.graphics.Color.green(argb).toString()
        blue = android.graphics.Color.blue(argb).toString()
    }
    fun applyRgb(r: String, g: String, b: String) {
        val rv = r.toIntOrNull()
        val gv = g.toIntOrNull()
        val bv = b.toIntOrNull()
        if (rv != null && gv != null && bv != null && rv in 0..255 && gv in 0..255 && bv in 0..255) {
            onColorChange(Color(android.graphics.Color.rgb(rv, gv, bv)))
        }
    }
    fun adjustBrightness(delta: Float) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[2] = (hsv[2] + delta).coerceIn(0.08f, 1f)
        onColorChange(Color(android.graphics.Color.HSVToColor(hsv)))
    }
    Column(Modifier.fillMaxWidth()) {
        Txt("roda de cores", color = p.accent, size = 11.sp, bold = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorWheel(color, onColorChange)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Box(Modifier.size(42.dp).background(color).border(1.dp, p.line))
                Spacer(Modifier.height(8.dp))
                Btn("mais claro", { adjustBrightness(0.10f) })
                Btn("mais escuro", { adjustBrightness(-0.10f) })
            }
        }
        TermField("hex", hex, { value ->
            hex = value
            val digits = value.trim().removePrefix("#")
            if (digits.matches(Regex("[0-9a-fA-F]{6}"))) {
                onColorChange(Color(android.graphics.Color.parseColor("#${digits}")))
            }
        })
        TermField("R", red, { red = it; applyRgb(it, green, blue) }, keyboardType = KeyboardType.Number)
        TermField("G", green, { green = it; applyRgb(red, it, blue) }, keyboardType = KeyboardType.Number)
        TermField("B", blue, { blue = it; applyRgb(red, green, it) }, keyboardType = KeyboardType.Number)
    }
}

@Composable
private fun ColorWheel(color: Color, onColorChange: (Color) -> Unit) {
    val selected = rememberUpdatedState(color)
    val change = rememberUpdatedState(onColorChange)
    val wheelColors = remember {
        (0..360 step 30).map { hue ->
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue.toFloat(), 1f, 1f)))
        }
    }
    Canvas(Modifier.size(210.dp).pointerInput(Unit) {
        detectTapGestures { point ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val dx = point.x - cx
            val dy = point.y - cy
            val radius = minOf(size.width, size.height) * 0.45f
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance <= radius) {
                val hue = (Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(selected.value.toArgb(), hsv)
                hsv[0] = hue
                hsv[1] = (distance / radius).coerceIn(0f, 1f)
                change.value(Color(android.graphics.Color.HSVToColor(hsv)))
            }
        }
    }) {
        val radius = minOf(size.width, size.height) * 0.45f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(brush = Brush.sweepGradient(wheelColors), radius = radius, center = center)
        // Fade to white towards the centre so the wheel also previews saturation.
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, Color.Transparent), center = center, radius = radius),
            radius = radius,
            center = center,
        )
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        val angle = Math.toRadians(hsv[0].toDouble())
        val markerRadius = radius * hsv[1]
        val marker = Offset(
            center.x + kotlin.math.cos(angle).toFloat() * markerRadius,
            center.y + kotlin.math.sin(angle).toFloat() * markerRadius,
        )
        drawCircle(Color.Black, radius = 9.dp.toPx(), center = marker)
        drawCircle(Color.White, radius = 6.dp.toPx(), center = marker)
        drawCircle(color, radius = 4.dp.toPx(), center = marker)
    }
}

/** Artist values shown as removable chips while keeping the editable source string. */
@Composable
fun ArtistChipsField(label: String, value: String, onChange: (String) -> Unit) {
    val artists = remember(value) { app.miglycal.data.ArtistNames.split(value) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        TermField(label, value, onChange)
        Flow {
            artists.forEach { artist ->
                Btn("× $artist", {
                    onChange(artists.filterNot { it.equals(artist, ignoreCase = true) }.joinToString(", "))
                }, size = 10.sp)
            }
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
