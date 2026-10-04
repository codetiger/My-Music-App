package com.codetiger.mymusicapp.setup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.codetiger.mymusicapp.data.AppSettings

enum class SetupStep {
    /** Let this app install its own updates ("Install unknown apps"). */
    AllowUpdates,

    /** Battery set to "No restrictions". */
    KeepPlaying,

    /** Xiaomi / Redmi / Poco only. */
    Autostart,

    /** Only when Android Auto is installed; written steps, no direct link. */
    AndroidAuto,
}

/** Phone Setup (FL-7): which steps apply, whether each is done, and the Android page for it. */
class PhoneSetup(private val context: Context) {
    private val packageName = context.packageName

    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco") ||
            Build.BRAND.lowercase() in setOf("xiaomi", "redmi", "poco")

    val hasAndroidAuto: Boolean
        get() = runCatching { context.packageManager.getPackageInfo(ANDROID_AUTO, 0); true }.getOrDefault(false)

    fun steps(): List<SetupStep> = buildList {
        add(SetupStep.AllowUpdates)
        add(SetupStep.KeepPlaying)
        if (isXiaomi) add(SetupStep.Autostart)
        if (hasAndroidAuto) add(SetupStep.AndroidAuto)
    }

    /** Whether Android allows checking this step; Autostart and Android Auto can't be checked. */
    fun isCheckable(step: SetupStep) = step == SetupStep.AllowUpdates || step == SetupStep.KeepPlaying

    fun isDone(step: SetupStep, settings: AppSettings): Boolean = when (step) {
        SetupStep.AllowUpdates -> context.packageManager.canRequestPackageInstalls()
        SetupStep.KeepPlaying -> context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        SetupStep.Autostart -> settings.autostartDone
        SetupStep.AndroidAuto -> settings.androidAutoDone
    }

    /** A step Android lets us check is off: "Music may stop when the screen is off" (HOME-4). */
    fun checkableStepOff(settings: AppSettings): Boolean =
        steps().any { isCheckable(it) && !isDone(it, settings) }

    fun stepsLeft(settings: AppSettings): Int = steps().count { !isDone(it, settings) }

    /** The Android page for a step, or null when there is no direct link. */
    fun intent(step: SetupStep): Intent? = when (step) {
        SetupStep.AllowUpdates -> Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
        SetupStep.KeepPlaying -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        SetupStep.Autostart -> Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        ).takeIf { context.packageManager.resolveActivity(it, 0) != null } ?: appDetails()
        SetupStep.AndroidAuto -> null
    }

    fun appDetails(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

    companion object {
        const val ANDROID_AUTO = "com.google.android.projection.gearhead"
    }
}
