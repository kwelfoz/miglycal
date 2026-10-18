package app.miglycal.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File

/** Leitura e escrita de etiquetas (ID3 em MP3, Vorbis comments em FLAC). */
object TagIO {

    fun read(path: String): Tags = runCatching {
        val tag = AudioFileIO.read(File(path)).tag
        fun g(k: FieldKey): String = runCatching { tag?.getFirst(k) }.getOrNull().orEmpty()
        Tags(
            title = g(FieldKey.TITLE),
            artist = g(FieldKey.ARTIST),
            album = g(FieldKey.ALBUM),
            albumArtist = g(FieldKey.ALBUM_ARTIST),
            year = g(FieldKey.YEAR),
            track = g(FieldKey.TRACK),
            disc = g(FieldKey.DISC_NO),
            genre = g(FieldKey.GENRE),
            comment = g(FieldKey.COMMENT),
        )
    }.getOrDefault(Tags())

    fun readLyrics(path: String): String? = runCatching {
        AudioFileIO.read(File(path)).tag?.getFirst(FieldKey.LYRICS)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /** Formato, sample rate e bitrate do ficheiro. */
    fun info(path: String): AudioInfo? {
        val ext = path.substringAfterLast('.').lowercase()
        return runCatching {
            val h = AudioFileIO.read(File(path)).audioHeader
            AudioInfo(ext, h.sampleRateAsNumber, h.bitRateAsNumber.toInt(), h.isVariableBitRate)
        }.getOrNull() ?: infoFallback(path, ext)
    }

    private fun infoFallback(path: String, ext: String): AudioInfo? = runCatching {
        val r = MediaMetadataRetriever()
        val kbps = try {
            r.setDataSource(path)
            (r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0) / 1000
        } finally {
            r.release()
        }
        val ex = MediaExtractor()
        val hz = try {
            ex.setDataSource(path)
            ex.getTrackFormat(0).getInteger(MediaFormat.KEY_SAMPLE_RATE)
        } finally {
            ex.release()
        }
        AudioInfo(ext, hz, kbps)
    }.getOrNull()

    /**
     * Grava sem acesso a todos os ficheiros: copia o ficheiro para a cache, edita lá e
     * escreve de volta através do URI (o utilizador autorizou antes via createWriteRequest).
     */
    fun writeViaUri(ctx: Context, uri: android.net.Uri, t: Tags, cover: ByteArray?, lyrics: String?) {
        val ext = (ctx.contentResolver.getType(uri) ?: "").let { if (it.contains("flac")) "flac" else "mp3" }
        val tmp = File(ctx.cacheDir, "edit_${System.nanoTime()}.$ext")
        try {
            val input = ctx.contentResolver.openInputStream(uri) ?: error("não foi possível ler o ficheiro")
            input.use { src -> tmp.outputStream().use { dst -> src.copyTo(dst) } }
            write(ctx, tmp.path, t, cover, lyrics)
            val out = ctx.contentResolver.openOutputStream(uri, "wt") ?: error("não foi possível escrever no ficheiro")
            out.use { dst -> tmp.inputStream().use { src -> src.copyTo(dst) } }
        } finally {
            tmp.delete()
        }
    }

    /** Grava etiquetas (e opcionalmente capa e letra) diretamente no ficheiro (Android 10 ou inferior). */
    fun write(ctx: Context, path: String, t: Tags, cover: ByteArray?, lyrics: String?) {
        val af = AudioFileIO.read(File(path))
        val tag = af.tagOrCreateAndSetDefault

        fun put(k: FieldKey, v: String) {
            if (v.isBlank()) runCatching { tag.deleteField(k) } else tag.setField(k, v.trim())
        }
        put(FieldKey.TITLE, t.title)
        put(FieldKey.ARTIST, t.artist)
        put(FieldKey.ALBUM, t.album)
        put(FieldKey.ALBUM_ARTIST, t.albumArtist)
        put(FieldKey.YEAR, t.year)
        put(FieldKey.TRACK, t.track)
        put(FieldKey.DISC_NO, t.disc)
        put(FieldKey.GENRE, t.genre)
        put(FieldKey.COMMENT, t.comment)
        if (lyrics != null) put(FieldKey.LYRICS, lyrics)

        if (cover != null) {
            val ext = if (cover.size > 4 && cover[1] == 0x50.toByte()) "png" else "jpg"
            val tmp = File.createTempFile("cover", ".$ext", ctx.cacheDir)
            try {
                tmp.writeBytes(cover)
                val art = ArtworkFactory.createArtworkFromFile(tmp)
                tag.deleteArtworkField()
                tag.setField(art)
            } finally {
                tmp.delete()
            }
        }
        af.commit()
    }
}
