package app.miglycal.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Capas embebidas nos ficheiros (com fallback para cover/folder.jpg na pasta). */
object Art {
    private val cache = LruCache<String, Bitmap>(24)

    suspend fun load(path: String, max: Int = 900): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(path) ?: runCatching { readEmbedded(path, max) ?: readFolderArt(path, max) }
            .getOrNull()
            ?.also { cache.put(path, it) }
    }

    fun invalidate(path: String) {
        cache.remove(path)
    }

    private fun readEmbedded(path: String, max: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(path)
            r.embeddedPicture?.let { decode(it, max) }
        } finally {
            r.release()
        }
    }

    private fun readFolderArt(path: String, max: Int): Bitmap? {
        val dir = File(path).parentFile ?: return null
        for (n in listOf("cover", "folder", "front", "album")) {
            for (e in listOf("jpg", "jpeg", "png")) {
                val f = File(dir, "$n.$e")
                if (f.exists()) return decode(f.readBytes(), max)
            }
        }
        return null
    }

    fun decode(bytes: ByteArray, max: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= max && bounds.outHeight / (sample * 2) >= max) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}
