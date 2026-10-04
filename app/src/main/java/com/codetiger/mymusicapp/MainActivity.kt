package com.codetiger.mymusicapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.codetiger.mymusicapp.data.AppSettings
import com.codetiger.mymusicapp.ui.IncomingShare
import com.codetiger.mymusicapp.ui.MyMusicApp
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MyMusicTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app by lazy { (application as MyMusicApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light bars always: the app ignores the phone's dark mode.
        val bars = SystemBarStyle.light(MusicColors.Surface.toArgb(), MusicColors.Surface.toArgb())
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        super.onCreate(savedInstanceState)
        app.player.connect()
        if (savedInstanceState == null) handleIntent(intent)

        lifecycleScope.launch {
            val first = app.settings.settings.first()
            announceUpdate(first)
            setContent {
                val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = first)
                MyMusicTheme(textSize = settings.textSize) {
                    MyMusicApp(app, startAtWelcome = !first.welcomeDone)
                }
            }
            app.appUpdater.checkIfDue()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** An update waiting to install goes in while the app is out of sight and nothing plays (UPD-3). */
    override fun onStop() {
        super.onStop()
        app.scope.launch { runCatching { app.appUpdater.installIfIdle(app.playbackActive.get()) } }
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            ACTION_OPEN_PLAYER -> app.ui.openPlayer.value = true
            Intent.ACTION_SEND -> {
                val stream = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                app.ui.incoming.value = when {
                    stream != null && intent.type?.startsWith("text/") != true -> IncomingShare.Files(listOf(stream))
                    text != null -> IncomingShare.Text(text)
                    stream != null -> IncomingShare.Files(listOf(stream))
                    else -> null
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val streams = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
                if (streams.isNotEmpty()) app.ui.incoming.value = IncomingShare.Files(streams)
            }
        }
    }

    /** "App updated", once, after a new version is installed (UPD-3). */
    private suspend fun announceUpdate(settings: AppSettings) {
        val seen = settings.lastSeenVersionCode
        if (seen != 0 && seen < BuildConfig.VERSION_CODE) app.ui.messages.show("App updated")
        if (seen != BuildConfig.VERSION_CODE) app.settings.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
    }

    companion object {
        const val ACTION_OPEN_PLAYER = "com.codetiger.mymusicapp.OPEN_PLAYER"
    }
}
