package com.github.dezzk.monjaro_music.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.dezzk.monjaro_music.data.Const
import com.github.dezzk.monjaro_music.data.State
import java.io.BufferedReader
import java.io.File
import java.io.FileFilter
import java.io.FileReader
import java.util.Arrays


/**
 * Created by muhammad.mohsen on 4/15/2017.
 */

/**
 * not really useful as a model class anymore, honestly...at the start of development, I used to display the track count for folders and the album/artist for tracks
 * now, only the statics are useful
 */
class ExplorerFile(
    pathname: String,
) : File(pathname) {

	// FileFilter implementation that accepts only visible directory/media files.
	private class ExplorerFileFilter : FileFilter {
		override fun accept(file: File): Boolean {
			return !file.isHidden && (file.isDirectory || MEDIA_EXTENSIONS.contains(file.extension.lowercase()))
		}
	}

    companion object {

		val ROOT: String = Environment.getExternalStorageDirectory().path // root directory (actually the internal storage directory!)

		// these consts help work around the nuisances of Android storage APIs
		const val ACTUAL_ROOT = "/storage"
		private const val EMULATED = "/storage/emulated"
		private const val EMULATED_ZERO = "/storage/emulated/0"

		val MEDIA_EXTENSIONS = listOf("mp3", "wav", "m4b", "m4a", "flac", "midi", "ogg", "opus", "aac") // supported media extensions

		private val filter = ExplorerFileFilter()

		fun isAtRoot(path: String?): Boolean {
			return (path?.filter { it == '/' }?.length ?: 1) <= 1
		}

		fun isTrack(f: File): Boolean {
			return f.exists() && MEDIA_EXTENSIONS.contains(f.extension.lowercase())
		}

		fun listExplorerFiles(path: String): ArrayList<ExplorerFile> {
			val fileModels = ArrayList<ExplorerFile>()

			var files = File(path).listFiles(filter)

			// just to make sure that we aren't trapped at the basement
			if (path == EMULATED && files == null)
				files = arrayOf(File(EMULATED_ZERO))
			else if (path == ACTUAL_ROOT && files == null) {
				files =
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
						listVolumes()
					else {
						// Pre-Android 11: try /storage listing, fall back to emulated + mounted volumes
						val mounted = findMountedVolumes()
						if (mounted.isNotEmpty())
							(listOf(File(EMULATED)) + mounted).toTypedArray()
						else
							arrayOf(File(EMULATED))
					}
			} else if (path == ACTUAL_ROOT && files != null) {
				// Even when /storage is listable, add any USB hub volumes not under /storage
				val mounted = findMountedVolumes().filter { !it.absolutePath.startsWith(ACTUAL_ROOT) }
				if (mounted.isNotEmpty()) files = files + mounted.toTypedArray()
			}

			if (files == null) return ArrayList()

			Arrays.sort(files) { o1, o2 ->
				if (o1.isDirectory && o2.isDirectory) o1.name.compareTo(o2.name, true) // if both are directories, compare their names
				else if (o1.isDirectory && !o2.isDirectory) -1 // if the first is a directory, it's always first
				else if (o2.isDirectory) 1 // if the second is a directory, it's always first
				else o1.name.compareTo(o2.name, true) // if both are tracks, compare their names
			}

            for (f in files) {
                fileModels.add(ExplorerFile(f.absolutePath))
            }
            return fileModels
        }

		// After the scoped storage changes, we can't access the SD card from "/storage".listFiles() anymore :)
		// this little guy returns them nonetheless
		@RequiresApi(Build.VERSION_CODES.R)
		private fun listVolumes(): Array<File> {
			val context = State.applicationContext
			val volumes = mutableSetOf<File>()

			// 1. StorageManager volumes (standard path)
			val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
			for (sv in storageManager.storageVolumes) {
				val dir = sv.directory ?: continue
				if (dir.absolutePath == EMULATED_ZERO) volumes.add(File(EMULATED))
				else volumes.add(dir)
			}

			// 2. Scan /proc/mounts for USB drives connected through a hub
			// These may not appear in StorageManager but are mounted by the kernel
			volumes.addAll(findMountedVolumes())

			return volumes.toTypedArray()
		}

		// Parses /proc/mounts to find externally mounted USB/SD volumes
		// that may not be reported by StorageManager (e.g. drives through a USB hub)
		private fun findMountedVolumes(): List<File> {
			val usbFileSystems = setOf("vfat", "exfat", "ntfs", "ext4", "fuseblk", "texfat", "sdfat")
			val ignoredPrefixes = listOf("/storage/emulated", "/data", "/system", "/cache", "/dev")
			val knownMountPrefixes = listOf("/mnt/media_rw/", "/mnt/usb/", "/mnt/USB/", "/mnt/udisk/", "/storage/")
			val result = mutableListOf<File>()

			try {
				BufferedReader(FileReader("/proc/mounts")).use { reader ->
					reader.forEachLine { line ->
						val parts = line.split(" ")
						if (parts.size < 3) return@forEachLine

						val mountPoint = parts[1]
						val fsType = parts[2]

						if (!usbFileSystems.contains(fsType)) return@forEachLine
						if (ignoredPrefixes.any { mountPoint.startsWith(it) }) return@forEachLine
						if (!knownMountPrefixes.any { mountPoint.startsWith(it) }) return@forEachLine

						val dir = File(mountPoint)
						if (dir.exists() && dir.canRead()) {
							result.add(dir)
						}
					}
				}
			} catch (e: Exception) {
				Log.w("ExplorerFile", "Failed to parse /proc/mounts", e)
			}

			return result
		}
	}
}

/**
 * Holds a track's chapter info
 */
class Chapter(val index: Int, val startTime: Long) {
	companion object {
		fun serialize(chapters: ArrayList<Chapter>): String {
			return chapters.joinToString(separator = "&&", transform = {
				it.index.toString() + "," + it.startTime.toString()
			})
		}

		fun deserialize(s: String?): ArrayList<Chapter> {
			if (s.isNullOrBlank()) return ArrayList()
			val chapters = s.split("&&").map {
				val props = it.split(",")
				Chapter(props.first().toInt(), props.last().toLong())
			}

			return ArrayList(chapters)
		}
	}
}
// the ArrayList is guaranteed to be sorted
fun ArrayList<Chapter>.getNextChapter(currentSeek: Long): Chapter? {
	return this.firstOrNull {
		it.startTime > currentSeek
	}
}
fun ArrayList<Chapter>.getPrevChapter(currentSeek: Long): Chapter {
	return this.last {
		it.startTime < currentSeek - Const.PREV_THRESHOLD // less than 5 seconds since the chapter began
	}
}

class SerializableBitmap(val data: ByteArray?) {
	private val decoded = BitmapFactory.decodeByteArray(data, 0, data?.size ?: 0)
	private val encoded = Base64.encodeToString(data, Base64.DEFAULT)

	val bitmap: Bitmap? = decoded
	val serialize: String = encoded
}
