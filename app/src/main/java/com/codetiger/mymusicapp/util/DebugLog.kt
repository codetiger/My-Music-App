package com.codetiger.mymusicapp.util

import android.util.Log
import com.codetiger.mymusicapp.BuildConfig

/** Logs in debug builds only. Release builds keep no logs of any kind (section 6, Privacy). */
object DebugLog {
    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d("MyMusic", message)
    }

    fun e(message: String, error: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e("MyMusic", message, error)
    }
}
