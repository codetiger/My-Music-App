package com.codetiger.mymusicapp

import android.app.Application
import com.codetiger.mymusicapp.update.DailyWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyMusicApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        DailyWorker.schedule(this)
        container.scope.launch(Dispatchers.IO) {
            runCatching { container.restoreCheck.run() }
            runCatching { container.library.purgeExpired() }
            // Unpack Python and ffmpeg early so the first song added starts sooner.
            runCatching { container.tools.ensureInitialized() }
            // The bundled yt-dlp ages quickly; fetch the newest on first run, then daily (UPD-4).
            container.ytDlpUpdater.checkIfDue()
        }
    }
}
