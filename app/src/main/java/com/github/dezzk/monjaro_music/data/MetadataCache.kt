package com.github.dezzk.monjaro_music.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.github.dezzk.monjaro_music.data.files.CachedFileMetadata
import kotlin.Int
import kotlin.Long
import kotlin.arrayOf

class MetadataCache(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_METADATA_SQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Nothing to upgrade yet
    }

    fun saveMetadataToCache(
        path: String,
        size: Long,
        metadata: CachedFileMetadata,
    ) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_ID, "$path:$size")
        values.put(KEY_TITLE, metadata.title)
        values.put(KEY_ARTIST, metadata.artist)
        values.put(KEY_ALBUM, metadata.album)
        values.put(KEY_DURATION, metadata.duration)

        db.insert(TABLE_METADATA, null, values)
        db.close()
    }

    fun getCachedMetadata(path: String, size: Long): CachedFileMetadata? {
        val cursor = this.readableDatabase.rawQuery(
            "SELECT $KEY_TITLE, $KEY_ARTIST, $KEY_ALBUM, $KEY_DURATION FROM $TABLE_METADATA WHERE $KEY_ID = ?",
            arrayOf("$path:$size")
        )

        val metadata = if (cursor.moveToFirst()) {
            CachedFileMetadata(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getLong(3),
            )
        } else {
            null
        }

        cursor.close()
        return metadata
    }

    companion object {
        private const val DATABASE_NAME = "metadata-cache.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_METADATA = "metadata"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_ALBUM = "album"
        private const val KEY_DURATION = "duration"

        private const val CREATE_TABLE_METADATA_SQL =
            "CREATE TABLE $TABLE_METADATA ($KEY_ID TEXT PRIMARY KEY, $KEY_TITLE TEXT, $KEY_ARTIST TEXT, $KEY_ALBUM TEXT, $KEY_DURATION INTEGER)"
    }
}