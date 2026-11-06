package dev.katsumi.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val breadcrumbs: String,
)

private val VIDEO_EXTS = setOf("mkv", "mp4", "webm", "avi", "m4v", "mov", "ts")

object Scanner {
    suspend fun scan(ctx: Context, treeUri: Uri): List<MediaFile> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(ctx, treeUri) ?: return@withContext emptyList()
        val out = mutableListOf<MediaFile>()
        walk(root, "", out)
        out.sortedWith(compareBy({ it.breadcrumbs }, { naturalKey(it.name) }))
    }

    private fun walk(dir: DocumentFile, trail: String, out: MutableList<MediaFile>) {
        val kids = try { dir.listFiles() } catch (_: Exception) { return }
        for (kid in kids) {
            if (kid.isDirectory) {
                walk(kid, if (trail.isEmpty()) kid.name.orEmpty() else "$trail / ${kid.name}", out)
            } else if (kid.isFile && kid.name?.substringAfterLast('.', "")?.lowercase() in VIDEO_EXTS) {
                out += MediaFile(kid.uri, kid.name.orEmpty(), kid.length(), trail)
            }
        }
    }

    /** Pads digit runs so Episode 2 sorts before Episode 15. */
    internal fun naturalKey(name: String): String =
        Regex("\\d+").replace(name.lowercase()) { it.value.padStart(8, '0') }
}
