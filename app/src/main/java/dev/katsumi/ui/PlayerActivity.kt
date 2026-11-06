package dev.katsumi.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import `is`.xyz.mpv.MPVLib
import java.io.File

class PlayerActivity : ComponentActivity() {

    companion object {
        fun intent(ctx: Context) = Intent(ctx, PlayerActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MPVLib.create(applicationContext)
        MPVLib.init()
        MPVLib.setPropertyString("vo", "gpu")
        MPVLib.setPropertyString("hwdec", "mediacodec")
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MpvScreen(onReady = { playBundledSample() })
            }
        }
    }

    private fun playBundledSample() {
        val out = File(cacheDir, "sample.mp4")
        if (!out.exists()) {
            assets.open("sample.mp4").use { input -> out.outputStream().use { input.copyTo(it) } }
        }
        MPVLib.command(arrayOf("loadfile", out.absolutePath))
    }

    override fun onDestroy() {
        MPVLib.destroy()
        super.onDestroy()
    }
}

@Composable
private fun MpvScreen(onReady: () -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            SurfaceView(ctx).also { view ->
                view.holder.addCallback(object : android.view.SurfaceHolder.Callback {
                    override fun surfaceCreated(h: android.view.SurfaceHolder) {
                        MPVLib.attachSurface(view.holder.surface)
                        onReady()
                    }
                    override fun surfaceChanged(h: android.view.SurfaceHolder, f: Int, w: Int, h2: Int) =
                        MPVLib.setPropertyString("android-surface-size", "${w}x$h2")
                    override fun surfaceDestroyed(h: android.view.SurfaceHolder) = MPVLib.detachSurface()
                })
            }
        },
    )
}
