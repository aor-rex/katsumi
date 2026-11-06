package dev.katsumi.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prefs by preferencesDataStore("katsumi")
private val FOLDER_URI = stringPreferencesKey("media_folder_uri")

class SettingsRepository(private val ctx: Context) {
    val folderUri: Flow<Uri?> = ctx.prefs.data.map { it[FOLDER_URI]?.let(Uri::parse) }

    suspend fun setFolder(uri: Uri) {
        ctx.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        ctx.prefs.edit { it[FOLDER_URI] = uri.toString() }
    }

    suspend fun clearFolder() {
        ctx.prefs.edit { it.remove(FOLDER_URI) }
    }
}
