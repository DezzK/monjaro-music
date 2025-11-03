package com.github.dezzk.monjaro_music.data.files

import com.github.dezzk.monjaro_music.core.ext.EMPTY
import android.media.MediaMetadataRetriever
import java.io.File


/**
 * Created by muhammad.mohsen on 11/3/2018.
 * uses ffmpeg metadata retriever because it can list chapters
 */

class FileMetadata(private val file: File) {

	init {
		retriever.setDataSource(file.path)
	}

	val title: String
		get() = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
			?: file.nameWithoutExtension

	val artist: String
		get() = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
			?: String.EMPTY

	val album: String
		get() = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
			?: file.parentFile?.name ?: String.EMPTY

	val duration: Long
		get() = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
			?: 0L

	companion object {
		private val retriever = MediaMetadataRetriever()
	}

}
