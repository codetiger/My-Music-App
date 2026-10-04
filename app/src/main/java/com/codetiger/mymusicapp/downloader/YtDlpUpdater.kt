package com.codetiger.mymusicapp.downloader

import android.content.Context
import com.codetiger.mymusicapp.util.Http
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File

/**
 * Keeps yt-dlp up to date without asking (UPD-4 to UPD-6): checks the stable channel daily,
 * verifies the checksum, tests the new version against the current one on a few links, keeps
 * the previous version, and switches back when new songs keep failing for site reasons.
 */
class YtDlpUpdater(
    private val context: Context,
    private val scripts: ScriptStore,
    private val ytDlp: YtDlp,
) {
    private val lock = Mutex()

    suspend fun checkForUpdate() = lock.withLock {
        val state = scripts.state()
        val release = JSONObject(Http.getString(LATEST_RELEASE))
        val tag = release.getString("tag_name")
        if (tag == state.active || tag == state.skipped) {
            scripts.update { it.copy(lastCheck = System.currentTimeMillis()) }
            return@withLock
        }
        val assets = release.getJSONArray("assets")
        fun asset(name: String): String? = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name") == name }?.getString("browser_download_url")
        val scriptUrl = asset(SCRIPT_ASSET) ?: return@withLock
        val sumsUrl = asset(SUMS_ASSET) ?: return@withLock

        // The new version must match the published checksum before it is even tried (UPD-5).
        val expected = Http.getString(sumsUrl).lines()
            .map { it.trim().split(Regex("\\s+")) }
            .firstOrNull { it.size == 2 && it[1].removePrefix("*") == SCRIPT_ASSET }
            ?.get(0)?.lowercase() ?: return@withLock
        val temp = File(context.cacheDir, "yt-dlp-$tag.download")
        val actual = Http.download(scriptUrl, temp)
        if (actual != expected) {
            temp.delete()
            return@withLock
        }
        val candidate = scripts.install(tag, temp)

        // Use it if it reads at least as many test links as the current one, so a fix still
        // lands while a site is broken for both.
        val newScore = score(candidate)
        val currentScore = score(scripts.activeScript())
        val adopt = newScore >= currentScore
        scripts.update {
            if (adopt) {
                it.copy(active = tag, previous = it.active ?: ScriptStore.BUNDLED, siteFailureStreak = 0, lastCheck = System.currentTimeMillis())
            } else {
                it.copy(lastCheck = System.currentTimeMillis())
            }
        }
        if (adopt) onNewVersion()
    }

    /** Called after switching versions, so songs waiting on a site fix try again now. */
    var onNewVersion: suspend () -> Unit = {}

    /** First run and then at most daily (UPD-4); the daily job also calls [checkForUpdate]. */
    suspend fun checkIfDue() {
        if (System.currentTimeMillis() - scripts.state().lastCheck < 23 * 60 * 60 * 1000L) return
        runCatching { checkForUpdate() }
    }

    fun recordSiteSuccess() {
        if (scripts.state().siteFailureStreak != 0) scripts.update { it.copy(siteFailureStreak = 0) }
    }

    /** Three site failures in a row: go back to the previous version if it still works (UPD-6). */
    suspend fun recordSiteFailure() = lock.withLock {
        scripts.update { it.copy(siteFailureStreak = it.siteFailureStreak + 1) }
        val state = scripts.state()
        if (state.siteFailureStreak < FAILURES_BEFORE_SWITCH) return@withLock
        val previous = state.previous
        if (state.active == null || previous == null) {
            scripts.update { it.copy(siteFailureStreak = 0) }
            return@withLock
        }
        val previousScore = score(scripts.script(previous))
        val currentScore = score(scripts.activeScript())
        val switchBack = previousScore > 0 && previousScore >= currentScore
        scripts.update {
            if (switchBack) {
                it.copy(
                    active = previous.takeUnless { p -> p == ScriptStore.BUNDLED },
                    previous = null,
                    skipped = it.active,
                    siteFailureStreak = 0,
                )
            } else {
                it.copy(siteFailureStreak = 0)
            }
        }
        if (switchBack) onNewVersion()
    }

    private suspend fun score(script: File): Int = TEST_LINKS.count { ytDlp.canRead(it, script) }

    companion object {
        private const val LATEST_RELEASE = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"
        private const val SCRIPT_ASSET = "yt-dlp"
        private const val SUMS_ASSET = "SHA2-256SUMS"
        private const val FAILURES_BEFORE_SWITCH = 3

        /** Stable public links: YouTube, YouTube Music, Facebook (UPD-5). */
        val TEST_LINKS = listOf(
            "https://www.youtube.com/watch?v=jNQXAC9IVRw",
            "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.facebook.com/watch/?v=10153231379946729",
        )
    }
}
