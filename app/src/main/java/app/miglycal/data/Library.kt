package app.miglycal.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import java.io.File

object Library {
    /** Lê todos os ficheiros .flac e .mp3 conhecidos pelo MediaStore. */
    fun scan(ctx: Context): List<Track> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
        )
        val sel = "(${MediaStore.Audio.Media.DATA} LIKE ? OR ${MediaStore.Audio.Media.DATA} LIKE ?)"
        val args = arrayOf("%.flac", "%.mp3")
        val out = ArrayList<Track>()

        fun clean(s: String?): String? = s?.takeIf { it.isNotBlank() && it != "<unknown>" }

        ctx.contentResolver.query(uri, proj, sel, args, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val path = c.getString(iData) ?: continue
                val id = c.getLong(iId)
                out += Track(
                    id = id,
                    uri = ContentUris.withAppendedId(uri, id).toString(),
                    path = path,
                    folder = File(path).parent ?: "",
                    title = clean(c.getString(iTitle)) ?: File(path).nameWithoutExtension,
                    artist = clean(c.getString(iArtist)) ?: "desconhecido",
                    album = clean(c.getString(iAlbum)) ?: "desconhecido",
                    durationMs = c.getLong(iDur),
                )
            }
        }
        return out.sortedBy { it.path.lowercase() }
    }

    fun shortPath(folder: String): String =
        folder.removePrefix("/storage/emulated/0/").removePrefix("/storage/emulated/0")
            .ifBlank { "/" }
}
