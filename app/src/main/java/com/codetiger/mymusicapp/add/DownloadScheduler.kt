package com.codetiger.mymusicapp.add

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/** One download job per song, run on any connection, retried when back online (ADD-10, ADD-12). */
class DownloadScheduler(context: Context) {
    private val work = WorkManager.getInstance(context)

    fun enqueue(songId: Long, replace: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_SONG_ID to songId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()
        work.enqueueUniqueWork(name(songId), if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(songId: Long) {
        work.cancelUniqueWork(name(songId))
    }

    private fun name(songId: Long) = "download-$songId"

    companion object {
        const val TAG = "download"
    }
}
