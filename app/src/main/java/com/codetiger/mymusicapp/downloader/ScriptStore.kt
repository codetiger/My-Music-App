package com.codetiger.mymusicapp.downloader

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Keeps the yt-dlp script versions: the one in use, the previous one (kept for switching
 * back, UPD-6) and a release to skip. Lives in no-backup storage, so a restored phone starts
 * again from the bundled script.
 */
class ScriptStore(context: Context, private val tools: NativeTools) {
    data class State(
        /** Release tag in use; null means the script bundled in the APK. */
        val active: String? = null,
        val previous: String? = null,
        val skipped: String? = null,
        val siteFailureStreak: Int = 0,
        val lastCheck: Long = 0,
    )

    private val root = File(context.noBackupFilesDir, "ytdlp").apply { mkdirs() }
    private val versionsDir = File(root, "versions").apply { mkdirs() }
    private val stateFile = File(root, "state.json")

    @Synchronized
    fun state(): State {
        if (!stateFile.exists()) return State()
        return runCatching {
            val o = JSONObject(stateFile.readText())
            State(
                active = o.optString("active").ifEmpty { null },
                previous = o.optString("previous").ifEmpty { null },
                skipped = o.optString("skipped").ifEmpty { null },
                siteFailureStreak = o.optInt("streak"),
                lastCheck = o.optLong("lastCheck"),
            )
        }.getOrDefault(State())
    }

    @Synchronized
    fun update(change: (State) -> State) {
        val s = change(state())
        val o = JSONObject()
            .put("active", s.active ?: "")
            .put("previous", s.previous ?: "")
            .put("skipped", s.skipped ?: "")
            .put("streak", s.siteFailureStreak)
            .put("lastCheck", s.lastCheck)
        val tmp = File(root, "state.json.tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(stateFile)
        // Old versions other than the current and previous are no longer needed.
        versionsDir.listFiles()?.filter { it.name != s.active && it.name != s.previous }?.forEach { it.deleteRecursively() }
    }

    /** The script for [tag]; null or [BUNDLED] means the one bundled in the APK. */
    fun script(tag: String?): File =
        if (tag == null || tag == BUNDLED) tools.bundledScript else File(File(versionsDir, tag), "yt-dlp")

    fun activeScript(): File {
        val tag = state().active
        val file = script(tag)
        return if (tag != null && file.exists()) file else tools.bundledScript
    }

    fun install(tag: String, downloaded: File): File {
        val dir = File(versionsDir, tag).apply { mkdirs() }
        val target = File(dir, "yt-dlp")
        downloaded.copyTo(target, overwrite = true)
        downloaded.delete()
        return target
    }

    companion object {
        /** "previous" value meaning the script bundled in the APK. */
        const val BUNDLED = "bundled"
    }
}
