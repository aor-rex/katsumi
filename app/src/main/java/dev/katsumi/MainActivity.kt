package dev.katsumi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.katsumi.data.MediaFile
import dev.katsumi.ui.LibraryViewModel
import dev.katsumi.ui.PlayerActivity
import dev.katsumi.ui.ScanState

private data class Tab(val label: String, val glyph: String)

private val TABS = listOf(
    Tab("Library", "▦"),
    Tab("Continue", "◷"),
    Tab("Review", "⚠"),
    Tab("Settings", "⚙"),
)

class MainActivity : ComponentActivity() {
    private val vm: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var tab by remember { mutableIntStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            TABS.forEachIndexed { i, t ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = { Text(t.glyph) },
                                    label = { Text(t.label) },
                                )
                            }
                        }
                    }
                ) { pad ->
                    Box(Modifier.padding(pad).fillMaxSize()) {
                        when (tab) {
                            0 -> LibraryScreen(vm)
                            1 -> PlaceholderScreen("Nothing in progress yet")
                            2 -> PlaceholderScreen("Review queue is empty")
                            3 -> SettingsScreen(vm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp)) { Text(text) }
}

@Composable
private fun LibraryScreen(vm: LibraryViewModel) {
    val state by vm.scan.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineMedium)
        when (val s = state) {
            is ScanState.NoFolder -> Text("No folder picked — open Settings and point Katsumi at your anime.")
            is ScanState.Scanning -> CircularProgressIndicator()
            is ScanState.Done -> {
                Text("${s.files.size} files", style = MaterialTheme.typography.bodyMedium)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.files, key = { it.uri.toString() }) { f -> FileRow(f) }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = { ctx.startActivity(PlayerActivity.intent(ctx)) }) {
            Text("▶ Play bundled sample")
        }
    }
}

@Composable
private fun FileRow(f: MediaFile) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(f.name, style = MaterialTheme.typography.bodyLarge)
            if (f.breadcrumbs.isNotEmpty()) {
                Text(f.breadcrumbs, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: LibraryViewModel) {
    val folder by vm.folderUri.collectAsStateWithLifecycle()
    val state by vm.scan.collectAsStateWithLifecycle()
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) vm.setFolder(uri) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Media folder", style = MaterialTheme.typography.titleMedium)
        Text(folder?.toString() ?: "none picked", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch(null) }) { Text("Change folder") }
            OutlinedButton(onClick = { vm.rescan() }, enabled = folder != null) { Text("Rescan") }
        }
        val count = (state as? ScanState.Done)?.files?.size
        if (count != null) Text("$count video files indexed")
    }
}
