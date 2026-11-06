package dev.katsumi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.katsumi.ui.PlayerActivity

private data class Tab(val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("Library", Icons.Filled.VideoLibrary),
    Tab("Continue", Icons.Filled.PlayCircle),
    Tab("Review", Icons.Filled.Rule),
    Tab("Settings", Icons.Filled.Settings),
)

class MainActivity : ComponentActivity() {
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
                                    icon = { Icon(t.icon, t.label) },
                                    label = { Text(t.label) },
                                )
                            }
                        }
                    }
                ) { pad ->
                    Box(Modifier.padding(pad).fillMaxSize()) {
                        when (tab) {
                            0 -> LibraryScreen()
                            1 -> PlaceholderScreen("Nothing in progress yet")
                            2 -> PlaceholderScreen("Review queue is empty")
                            3 -> PlaceholderScreen("Settings — folder picker lands here")
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
private fun LibraryScreen() {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineMedium)
        Text("Milestone 1 — tap below to verify MPV first frame", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { ctx.startActivity(PlayerActivity.intent(ctx)) }) {
            Text("▶ Play bundled sample")
        }
    }
}
