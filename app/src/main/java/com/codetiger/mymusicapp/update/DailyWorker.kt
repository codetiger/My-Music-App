package com.codetiger.mymusicapp.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.codetiger.mymusicapp.MyMusicApplication
import java.util.concurrent.TimeUnit

/**
 * Once a day: empty what has been in Recently Removed for 30 days, update yt-dlp silently
 * (UPD-4) and check for a new app version (UPD-2).
 */
class DailyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val app = (context.applicationContext as MyMusicApplication).container

    override suspend fun doWork(): Result {
        runCatching { app.library.purgeExpired() }
        runCatching { app.ytDlpUpdater.checkForUpdate() }
        runCatching {
            app.appUpdater.checkIfDue()
            app.appUpdater.installIfIdle(app.playbackActive.get())
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
