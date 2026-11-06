package dev.katsumi.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.katsumi.data.MediaFile
import dev.katsumi.data.Scanner
import dev.katsumi.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object NoFolder : ScanState
    data object Scanning : ScanState
    data class Done(val files: List<MediaFile>) : ScanState
}

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    val folderUri: StateFlow<Uri?> = repo.folderUri.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _scan = MutableStateFlow<ScanState>(ScanState.NoFolder)
    val scan: StateFlow<ScanState> = _scan.asStateFlow()
    private var scannedFor: Uri? = null

    init {
        viewModelScope.launch {
            folderUri.collect { uri ->
                if (uri == null) {
                    _scan.value = ScanState.NoFolder
                    scannedFor = null
                } else if (uri != scannedFor) {
                    rescan(uri)
                }
            }
        }
    }

    fun setFolder(uri: Uri) {
        viewModelScope.launch {
            repo.setFolder(uri)
        }
    }

    fun rescan() {
        folderUri.value?.let { rescan(it) }
    }

    private fun rescan(uri: Uri) {
        viewModelScope.launch {
            scannedFor = uri
            _scan.value = ScanState.Scanning
            _scan.value = ScanState.Done(Scanner.scan(getApplication(), uri))
        }
    }
}
