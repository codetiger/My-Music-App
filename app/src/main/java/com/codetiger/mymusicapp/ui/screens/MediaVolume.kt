package com.codetiger.mymusicapp.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** The phone's media volume, the same one the volume buttons move (PLY-4). */
class MediaVolume(private val audio: AudioManager) {
    var level by mutableFloatStateOf(read())
        private set

    private fun read(): Float {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    fun refresh() {
        level = read()
    }

    fun set(fraction: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(fraction * max), 0)
        refresh()
    }

    val steps: Int get() = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
}

@Composable
fun rememberMediaVolume(): MediaVolume {
    val context = LocalContext.current
    val volume = remember { MediaVolume(context.getSystemService(AudioManager::class.java)) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) = volume.refresh()
        }
        context.registerReceiver(receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"), Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return volume
}
