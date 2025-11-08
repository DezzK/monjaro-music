package com.github.dezzk.monjaro_music.data.files

import android.content.Context
import android.media.MediaMetadataRetriever
import com.github.dezzk.monjaro_music.core.ext.EMPTY
import com.github.dezzk.monjaro_music.data.MetadataCache
import java.io.File


/**
 * Created by muhammad.mohsen on 11/3/2018.
 * uses ffmpeg metadata retriever because it can list chapters
 */

data class CachedFileMetadata(
    val title: String, val artist: String, val album: String, val duration: Long
) {
    fun getTrackTitle(file: File): String {
        val title = title.trim().ifEmpty { file.nameWithoutExtension }
        val artist = artist.trim()
        return if (artist.isEmpty()) title else "$artist - $title"
    }
}

object FileMetadata {
    private val retriever = MediaMetadataRetriever()
    private lateinit var metadataCache: MetadataCache

    fun initialize(applicationContext: Context) {
        metadataCache = MetadataCache(applicationContext)
    }

    fun getCachedMetadata(file: File): CachedFileMetadata? {
        val cachedMetadata = if (::metadataCache.isInitialized) {
            metadataCache.getCachedMetadata(file.absolutePath, file.length())
        } else {
            null
        }

        return cachedMetadata
    }

    fun retrieveMetadata(file: File): CachedFileMetadata {
        val metadata = synchronized(retriever) {
            retriever.setDataSource(file.path)
            CachedFileMetadata(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
                    ?: file.nameWithoutExtension,
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
                    ?: String.EMPTY,
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
                    ?: file.parentFile?.name ?: String.EMPTY,
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                    ?: 0L,
            )
        }

        return metadata
    }

    fun cacheMetadata(file: File, metadata: CachedFileMetadata) {
        if (::metadataCache.isInitialized) {
            metadataCache.saveMetadataToCache(file.absolutePath, file.length(), metadata)
        }
    }

    fun getMetadata(file: File): CachedFileMetadata {
        val cachedMetadata = getCachedMetadata(file)
        if (cachedMetadata != null) {
            return cachedMetadata
        }

        val metadata = retrieveMetadata(file);
        cacheMetadata(file, metadata)

        return metadata
    }
}
