package com.codetiger.mymusicapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Song::class, Playlist::class, PlaylistSong::class], version = 1)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songs(): SongDao
    abstract fun playlists(): PlaylistDao

    companion object {
        fun create(context: Context): MusicDatabase =
            Room.databaseBuilder(context, MusicDatabase::class.java, "music.db")
                .addCallback(object : Callback() {
                    // Favourites always exists and cannot be deleted (PL-1).
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "INSERT INTO playlist (name, built_in, position, created_at) VALUES ('Favourites', 'FAVOURITES', 0, ?)",
                            arrayOf(System.currentTimeMillis()),
                        )
                    }
                })
                .build()
    }
}
