package com.codetiger.mymusicapp.downloader

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Runs the Python, yt-dlp and ffmpeg binaries bundled by youtubedl-android.
 *
 * The library only unpacks them here; we start the processes ourselves so any yt-dlp
 * script version can be run, which the side-by-side update test needs (UPD-5).
 */
class NativeTools(private val context: Context) {
    data class Output(val exitCode: Int, val out: String, val err: String)

    private val initLock = Mutex()
    @Volatile private var initialized = false

    private val binDir = File(context.applicationInfo.nativeLibraryDir)
    private val baseDir = File(context.noBackupFilesDir, YoutubeDL.baseName)
    private val packagesDir = File(baseDir, "packages")
    private val pythonHome = File(packagesDir, "python/usr")
    private val python = File(binDir, "libpython.so")
    private val quickJs = File(binDir, "libqjs.so")
    private val ffmpeg = File(binDir, "libffmpeg.so")
    private val ytDlpCache = File(context.cacheDir, "yt-dlp")

    /** The yt-dlp script that ships inside this APK (UPD-7). */
    val bundledScript = File(File(baseDir, YoutubeDL.ytdlpDirName), YoutubeDL.ytdlpBin)

    suspend fun ensureInitialized() = initLock.withLock {
        if (initialized) return@withLock
        withContext(Dispatchers.IO) {
            try {
                YoutubeDL.getInstance().init(context)
                FFmpeg.getInstance().init(context)
            } catch (e: Exception) {
                // YoutubeDLException is not an IOException: report it like a tool that couldn't start,
                // so callers handle it as "try later" instead of crashing.
                throw ToolException("Could not unpack the tools", e)
            }
        }
        initialized = true
    }

    private fun toolEnvironment(): Map<String, String> = mapOf(
        "LD_LIBRARY_PATH" to listOf("python", "ffmpeg", "aria2c")
            .joinToString(":") { File(packagesDir, "$it/usr/lib").absolutePath },
        "SSL_CERT_FILE" to File(pythonHome, "etc/tls/cert.pem").absolutePath,
        "PATH" to (System.getenv("PATH") ?: "") + ":" + binDir.absolutePath,
        "PYTHONHOME" to pythonHome.absolutePath,
        "HOME" to pythonHome.absolutePath,
        "TMPDIR" to context.cacheDir.absolutePath,
    )

    /** Runs yt-dlp [script] with [args]. Each stdout line goes to [onLine] as it arrives. */
    suspend fun ytDlp(script: File, args: List<String>, onLine: (String) -> Unit = {}): Output {
        ensureInitialized()
        val command = buildList {
            add(python.absolutePath)
            add(script.absolutePath)
            addAll(listOf("--no-config", "--cache-dir", ytDlpCache.absolutePath))
            addAll(listOf("--js-runtimes", "quickjs:${quickJs.absolutePath}"))
            addAll(listOf("--ffmpeg-location", ffmpeg.absolutePath))
            addAll(args)
        }
        return run(command, onLine)
    }

    suspend fun ffmpeg(args: List<String>): Output {
        ensureInitialized()
        return run(listOf(ffmpeg.absolutePath, "-hide_banner", "-nostdin", "-y") + args) {}
    }

    private suspend fun run(command: List<String>, onLine: (String) -> Unit): Output = withContext(Dispatchers.IO) {
        val process = try {
            ProcessBuilder(command).apply { environment().putAll(toolEnvironment()) }.start()
        } catch (e: IOException) {
            throw ToolException("Could not start ${command.first()}", e)
        }
        val err = StringBuilder()
        val errReader = Thread {
            process.errorStream.bufferedReader().useLines { lines -> lines.forEach { err.appendLine(it) } }
        }.apply { start() }
        // Cancelling the coroutine stops the process (and unblocks the reader below).
        val cancelHandle = currentCoroutineContext()[Job]?.invokeOnCompletion { cause ->
            if (cause != null) kill(process)
        }
        val out = StringBuilder()
        try {
            process.inputStream.bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    out.appendLine(line)
                    onLine(line)
                }
            }
            currentCoroutineContext().ensureActive()
            val exit = process.waitFor()
            errReader.join()
            Output(exit, out.toString(), err.toString())
        } finally {
            cancelHandle?.dispose()
            kill(process)
        }
    }

    /** Stops the process and anything it started (yt-dlp runs QuickJS and ffmpeg as children). */
    private fun kill(process: Process) {
        if (!process.isAlive) return
        pidOf(process)?.let { pid -> descendants(pid).forEach { android.os.Process.sendSignal(it, SIGKILL) } }
        process.destroyForcibly()
    }

    private fun pidOf(process: Process): Int? = runCatching {
        process.javaClass.getDeclaredField("pid").apply { isAccessible = true }.getInt(process)
    }.getOrNull()

    private fun descendants(root: Int): List<Int> {
        val parents = File("/proc").listFiles { f -> f.name.all(Char::isDigit) }.orEmpty().mapNotNull { dir ->
            runCatching {
                // /proc/<pid>/stat: "pid (name) state ppid ..."; the name may contain spaces.
                val stat = File(dir, "stat").readText()
                dir.name.toInt() to stat.substringAfterLast(')').trim().split(' ')[1].toInt()
            }.getOrNull()
        }
        val result = mutableListOf<Int>()
        var frontier = listOf(root)
        while (frontier.isNotEmpty()) {
            frontier = parents.filter { it.second in frontier }.map { it.first }
            result += frontier
        }
        return result
    }

    private companion object {
        const val SIGKILL = 9
    }
}

class ToolException(message: String, cause: Throwable? = null) : IOException(message, cause)
