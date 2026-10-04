package com.codetiger.mymusicapp

import android.app.Application
import com.codetiger.mymusicapp.add.DownloadScheduler
import com.codetiger.mymusicapp.add.FileImporter
import com.codetiger.mymusicapp.add.InfoCache
import com.codetiger.mymusicapp.add.SongAdder
import com.codetiger.mymusicapp.data.AppFiles
import com.codetiger.mymusicapp.data.LibraryRepository
import com.codetiger.mymusicapp.data.PlayerStateStore
import com.codetiger.mymusicapp.data.RestoreCheck
import com.codetiger.mymusicapp.data.SettingsRepository
import com.codetiger.mymusicapp.data.db.MusicDatabase
import com.codetiger.mymusicapp.downloader.ActiveDownloads
import com.codetiger.mymusicapp.downloader.HttpDownloader
import com.codetiger.mymusicapp.downloader.NativeTools
import com.codetiger.mymusicapp.downloader.ScriptStore
import com.codetiger.mymusicapp.downloader.YtDlp
import com.codetiger.mymusicapp.downloader.YtDlpUpdater
import com.codetiger.mymusicapp.photo.DrawingMaker
import com.codetiger.mymusicapp.photo.HomeShortcut
import com.codetiger.mymusicapp.photo.PhotoStore
import com.codetiger.mymusicapp.player.PlayerConnection
import com.codetiger.mymusicapp.setup.PhoneSetup
import com.codetiger.mymusicapp.ui.UiState
import com.codetiger.mymusicapp.update.AppUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.atomic.AtomicBoolean

/** Everything the app is made of, created once. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val files = AppFiles(app)
    val db = MusicDatabase.create(app)
    val settings = SettingsRepository(app)
    val playerState = PlayerStateStore(app)
    val library = LibraryRepository(db, files)

    val tools = NativeTools(app)
    val scripts = ScriptStore(app, tools)
    val ytDlp = YtDlp(tools, scripts)
    val ytDlpUpdater = YtDlpUpdater(app, scripts, ytDlp)
    val httpDownloader = HttpDownloader()
    val activeDownloads = ActiveDownloads()
    val infoCache = InfoCache()
    val scheduler = DownloadScheduler(app)
    val songAdder = SongAdder(db, library, ytDlp, infoCache, scheduler)
    val fileImporter = FileImporter(app, db, library, files, tools)

    /** Set by the player service; app updates wait while music plays (UPD-3). */
    val playbackActive = AtomicBoolean(false)
    val player = PlayerConnection(app, scope)

    val shortcut = HomeShortcut(app, files)
    val photoStore = PhotoStore(files, settings, shortcut)
    val drawingMaker = DrawingMaker(app)
    val phoneSetup = PhoneSetup(app)
    val appUpdater = AppUpdater(app, files, settings)
    val restoreCheck = RestoreCheck(db, library, settings, scheduler)

    /** Messages, shares and the photo draft, shared between screens. */
    val ui = UiState()

    init {
        // A new downloader may fix a site: songs waiting to download try again straight away.
        ytDlpUpdater.onNewVersion = {
            db.songs().pendingDownloads().forEach { scheduler.enqueue(it.id, replace = true) }
        }
    }
}
