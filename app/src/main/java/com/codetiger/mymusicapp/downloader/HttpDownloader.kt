package com.codetiger.mymusicapp.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Fetches a direct audio URL into a file in order, front to back, so it can be played while it
 * downloads. Resumes a partly written file. Uses ranged requests in chunks where the site wants
 * them (YouTube throttles single long requests).
 */
class HttpDownloader {

    suspend fun download(
        format: MediaInfo.AudioFormat,
        part: File,
        onProgress: (written: Long, total: Long?) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        var written = if (part.exists()) part.length() else 0L
        var total: Long? = format.sizeBytes
        onProgress(written, total)
        FileOutputStream(part, true).use { out ->
            while (total == null || written < total!!) {
                ensureActive()
                val end = format.chunkSize?.let { written + it - 1 }
                val connection = open(format, written, end)
                try {
                    when (val code = connection.responseCode) {
                        HttpURLConnection.HTTP_OK -> {
                            if (written > 0) {
                                // The server ignored the range: start again from the beginning.
                                out.channel.truncate(0)
                                written = 0
                            }
                            total = connection.contentLengthLong.takeIf { it > 0 } ?: total
                        }
                        HttpURLConnection.HTTP_PARTIAL -> {
                            total = parseTotal(connection.getHeaderField("Content-Range")) ?: total
                        }
                        416 -> if (total == null || written >= total!!) return@withContext else throw DownloadError.Site("Range refused")
                        // The direct URL expired (they last a few hours) or the site refused it.
                        401, 403, 404, 410 -> throw DownloadError.Site("HTTP $code")
                        else -> throw DownloadError.Site("HTTP $code")
                    }
                    val before = written
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(BUFFER)
                        while (true) {
                            ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            out.write(buffer, 0, n)
                            written += n
                            onProgress(written, total)
                        }
                    }
                    out.flush()
                    // Without ranges (or without a known length) one response is the whole file.
                    if (end == null || total == null) return@withContext
                    if (written == before) throw DownloadError.Site("Empty response")
                } catch (e: DownloadError) {
                    throw e
                } catch (e: IOException) {
                    throw classify(e)
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    private fun open(format: MediaInfo.AudioFormat, from: Long, to: Long?): HttpURLConnection = try {
        (URL(format.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            format.headers.forEach { (k, v) -> setRequestProperty(k, v) }
            if (from > 0 || to != null) setRequestProperty("Range", "bytes=$from-${to ?: ""}")
            setRequestProperty("Accept-Encoding", "identity")
        }
    } catch (e: IOException) {
        throw classify(e)
    }

    private fun classify(e: IOException): IOException = when (e) {
        is UnknownHostException, is ConnectException, is InterruptedIOException, is SocketException, is SSLException ->
            DownloadError.Offline(e.message ?: e.javaClass.simpleName)
        else -> if (e.message?.contains("ENOSPC") == true || e.message?.contains("No space") == true) {
            DownloadError.NoSpace(e.message ?: "")
        } else DownloadError.Offline(e.message ?: e.javaClass.simpleName)
    }

    private fun parseTotal(contentRange: String?): Long? =
        contentRange?.substringAfterLast('/')?.trim()?.toLongOrNull()

    private companion object {
        const val BUFFER = 64 * 1024
    }
}
