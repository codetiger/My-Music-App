package com.codetiger.mymusicapp.photo

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import com.codetiger.mymusicapp.MainActivity
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.AppFiles

/** The home-screen picture: a pinned shortcut with the drawing as its icon (FL-8 to FL-10). */
class HomeShortcut(private val context: Context, private val files: AppFiles) {
    private val manager = context.getSystemService(ShortcutManager::class.java)

    /** Some launchers can't pin shortcuts; then the Home card isn't shown (HOME-5). */
    val isSupported: Boolean get() = manager.isRequestPinShortcutSupported

    val isPinned: Boolean
        get() = manager.getShortcuts(ShortcutManager.FLAG_MATCH_PINNED).any { it.id == ID }

    /** Asks Android to place the shortcut; Android asks the user for one confirmation tap. */
    fun request(userName: String?): Boolean = manager.requestPinShortcut(info(userName), null)

    /** Changing the name or photo updates the shortcut straight away (FL-10). */
    fun update(userName: String?) {
        if (isPinned) manager.updateShortcuts(listOf(info(userName)))
    }

    private fun info(userName: String?): ShortcutInfo {
        val (short, long) = labels(userName)
        val bitmap = files.drawingIcon.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }
        val icon = bitmap?.let { Icon.createWithAdaptiveBitmap(it) }
            ?: Icon.createWithResource(context, R.mipmap.ic_launcher)
        return ShortcutInfo.Builder(context, ID)
            .setShortLabel(short)
            .setLongLabel(long)
            .setIcon(icon)
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
            .build()
    }

    companion object {
        private const val ID = "home"
        private const val SHORT_LABEL_MAX = 12

        /** "Murali's Music" when it fits in 12 characters, otherwise the name alone; long label "Murali's Music App". */
        fun labels(userName: String?): Pair<String, String> {
            if (userName.isNullOrBlank()) return "My Music" to "My Music App"
            val music = "$userName's Music"
            return (if (music.length <= SHORT_LABEL_MAX) music else userName) to "$music App"
        }
    }
}
