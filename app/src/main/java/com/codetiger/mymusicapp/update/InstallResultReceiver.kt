package com.codetiger.mymusicapp.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.codetiger.mymusicapp.MyMusicApplication

/** Hears how an app update install went. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        (context.applicationContext as MyMusicApplication).container.appUpdater.onInstallResult(status, confirm)
    }
}
