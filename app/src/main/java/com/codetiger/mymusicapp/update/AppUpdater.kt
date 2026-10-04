package com.codetiger.mymusicapp.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import com.codetiger.mymusicapp.BuildConfig
import com.codetiger.mymusicapp.data.AppFiles
import com.codetiger.mymusicapp.data.SettingsRepository
import com.codetiger.mymusicapp.util.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * App updates from this project's GitHub Releases (UPD-1 to UPD-3): checks at most once a day,
 * downloads in the background, installs without a tap where Android allows it and only while
 * nothing is playing. Otherwise Home shows "A new version is ready" and Android asks once.
 */
class AppUpdater(
    private val context: Context,
    private val files: AppFiles,
    private val settings: SettingsRepository,
) {
    sealed interface State {
        data object None : State
        /** Downloaded and checked; waiting for a moment with no music. */
        data class Ready(val apk: File, val versionName: String) : State
        /** Android wants one confirmation tap. */
        data class NeedsTap(val apk: File, val versionName: String, val confirm: Intent?) : State
    }

    private val _state = MutableStateFlow<State>(State.None)
    val state: StateFlow<State> = _state.asStateFlow()
    private val lock = Mutex()

    init {
        readyApk()?.let { (apk, name) -> _state.value = State.Ready(apk, name) }
    }

    /** On launch and from the daily job: at most one check a day (UPD-2). */
    suspend fun checkIfDue() {
        val last = settings.current().lastAppUpdateCheck
        if (System.currentTimeMillis() - last < TimeUnit.HOURS.toMillis(23)) return
        runCatching { check() }
    }

    suspend fun check() = lock.withLock {
        val release = JSONObject(Http.getString("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"))
        settings.setLastAppUpdateCheck(System.currentTimeMillis())
        val assets = release.getJSONArray("assets")
        val apkUrl = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith(".apk") }
            ?.getString("browser_download_url") ?: return@withLock
        val tag = release.getString("tag_name")
        if (!isNewer(tag.removePrefix("v"), BuildConfig.VERSION_NAME)) return@withLock
        if (readyApk()?.second == tag) return@withLock

        files.updatesDir.listFiles()?.forEach { it.delete() }
        val apk = File(files.updatesDir, "update-$tag.apk")
        Http.download(apkUrl, apk)
        // Only an APK of this app with a higher version is kept.
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
        if (info == null || info.packageName != context.packageName || info.longVersionCode <= BuildConfig.VERSION_CODE) {
            apk.delete()
            return@withLock
        }
        _state.value = State.Ready(apk, tag)
    }

    /** Installs a downloaded update if nothing is playing (UPD-3). */
    suspend fun installIfIdle(isPlaying: Boolean) {
        val ready = _state.value as? State.Ready ?: return
        if (isPlaying) return
        install(ready.apk)
    }

    /** Install tapped on the Home card. */
    suspend fun installNow() {
        when (val s = _state.value) {
            is State.NeedsTap -> s.confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } ?: install(s.apk)
            is State.Ready -> install(s.apk)
            State.None -> Unit
        }
    }

    private suspend fun install(apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            setInstallReason(PackageManager.INSTALL_REASON_USER)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("app.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, InstallResultReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context, sessionId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pending.intentSender)
        }
    }

    /** Called by [InstallResultReceiver]. */
    fun onInstallResult(status: Int, confirm: Intent?) {
        val current = _state.value
        val (apk, name) = when (current) {
            is State.Ready -> current.apk to current.versionName
            is State.NeedsTap -> current.apk to current.versionName
            State.None -> return
        }
        _state.value = when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> State.NeedsTap(apk, name, confirm)
            PackageInstaller.STATUS_SUCCESS -> State.None
            // Cancelled or failed: offer the Install card so the user can try with one tap.
            else -> State.NeedsTap(apk, name, null)
        }
    }

    private fun readyApk(): Pair<File, String>? {
        val apk = files.updatesDir.listFiles()?.firstOrNull { it.name.startsWith("update-") && it.name.endsWith(".apk") } ?: return null
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
        if (info == null || info.longVersionCode <= BuildConfig.VERSION_CODE) {
            apk.delete()
            return null
        }
        return apk to apk.name.removePrefix("update-").removeSuffix(".apk")
    }

    companion object {
        /** Compares dotted versions: "1.10.0" is newer than "1.9.2". */
        fun isNewer(candidate: String, current: String): Boolean {
            val a = candidate.split('.', '-').map { it.toIntOrNull() ?: 0 }
            val b = current.split('.', '-').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
