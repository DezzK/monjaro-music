package com.github.dezzk.monjaro_music.app.main

import abak.tr.com.boxedverticalseekbar.VolumeControl
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.dezzk.monjaro_music.app.appbar.AppBarManager
import com.github.dezzk.monjaro_music.app.explorer.ExplorerManager
import com.github.dezzk.monjaro_music.app.player.PlaybackManager
import com.github.dezzk.monjaro_music.app.player.PlayerControlsManager2
import com.github.dezzk.monjaro_music.app.settings.SettingsManager
import com.github.dezzk.monjaro_music.core.evt.EventBus
import com.github.dezzk.monjaro_music.data.EventSource
import com.github.dezzk.monjaro_music.data.EventType
import com.github.dezzk.monjaro_music.data.State
import com.github.dezzk.monjaro_music.data.SystemEvent
import com.github.dezzk.monjaro_music.data.files.ExplorerFile
import com.github.dezzk.monjaro_music.databinding.MainFragmentBinding
import java.lang.ref.WeakReference


class MainFragment : Fragment() {

	private lateinit var binding: MainFragmentBinding
	private lateinit var permissionRequest: ActivityResultLauncher<String>
	private val VOLUME_STREAM = AudioManager.STREAM_MUSIC
	private lateinit var audioManager: AudioManager
	private lateinit var volumeCheckRunnable: Runnable
	private val mainHandler = Handler(Looper.getMainLooper())

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// storage permission...must be in onCreate
		permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
			if (isGranted) {
				initialize()

			} else {
				binding.layoutPermission.root.visibility = View.VISIBLE
				binding.layoutPermission.buttonGrantPermission.setOnClickListener {
					initialize()
				}
			}
		}

		// back button
		requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				onBackPressed()
			}
		})
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		binding = MainFragmentBinding.inflate(inflater, container, false)
		initialize()

		return binding.root
	}

	override fun onStart() {
		super.onStart()
		EventBus.send(SystemEvent(EventSource.FRAGMENT, EventType.APP_FOREGROUNDED))
	}

	// initializes everything except the service!
	private fun initialize() {
		when {
			ContextCompat.checkSelfPermission(requireContext(), DISK_PERMISSION) == PackageManager.PERMISSION_GRANTED -> {
				binding.layoutPermission.root.visibility = View.GONE

				// breadcrumbs
				val appBarManager = AppBarManager(binding)
				appBarManager.initialize()

				// explorer
				val explorerManager = ExplorerManager(binding.recyclerViewExplorer)
				explorerManager.initialize()

				// volume control
				audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
				binding.volumeControl.min = audioManager.getStreamMinVolume(VOLUME_STREAM)
				binding.volumeControl.max = audioManager.getStreamMaxVolume(VOLUME_STREAM)
				binding.volumeControl.value = audioManager.getStreamVolume(VOLUME_STREAM)
				binding.volumeControl.setOnBoxedPointsChangeListener(object : VolumeControl.OnValuesChangeListener {
					override fun onPointsChanged(volumeControl: VolumeControl?, points: Int) {
						audioManager.setStreamVolume(VOLUME_STREAM, points, 0)
					}

					override fun onStartTrackingTouch(boxedPoints: VolumeControl?) {}

					override fun onStopTrackingTouch(boxedPoints: VolumeControl?) {}
				})

				volumeCheckRunnable = object : Runnable {
					override fun run() {
						binding.volumeControl.value = audioManager.getStreamVolume(VOLUME_STREAM)
						mainHandler.postDelayed(this, 500)
					}
				}
				mainHandler.postDelayed(volumeCheckRunnable , 500)

				// controls
				val playerControlsManager = PlayerControlsManager2(binding)
				playerControlsManager.initialize()

				// settings
				val settingsManager = SettingsManager(binding)
				settingsManager.initialize()

				// service
				val playerIntent = Intent(requireActivity(), PlaybackManager::class.java)
				ContextCompat.startForegroundService(requireActivity(), playerIntent)

				// restore the state, now that everything is initialized (except the service...which is why restoreState is manually called over there)
				if (State.Track.exists) EventBus.send(SystemEvent(EventSource.FRAGMENT, EventType.METADATA_UPDATE))

				State.activity = WeakReference(requireActivity())
			}
			shouldShowRequestPermissionRationale(DISK_PERMISSION) -> {
				permissionRequest.launch(DISK_PERMISSION)
			}
			else -> {
				permissionRequest.launch(DISK_PERMISSION)
			}
		}
	}

	private fun onBackPressed() {
		when {
			State.isSettingsSheetVisible -> EventBus.send(SystemEvent(EventSource.FRAGMENT, EventType.HIDE_SETTINGS))
			State.isSelectModeActive or State.isSearchModeActive -> {
				State.selectedTracks.clear() // update the state
				State.isSearchModeActive = false
				EventBus.send(SystemEvent(EventSource.FRAGMENT, EventType.SELECT_MODE_INACTIVE))
			}
			ExplorerFile.isAtRoot(State.currentDirectory.absolutePath) -> {
				requireActivity().moveTaskToBack(true)
			}
			else -> {
				State.currentDirectory = State.currentDirectory.parentFile!! // don't worry about it
				EventBus.send(SystemEvent(EventSource.FRAGMENT, EventType.DIR_CHANGE, State.currentDirectory.absolutePath))
			}
		}
	}

	companion object {
		fun newInstance() = MainFragment()

		val DISK_PERMISSION = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
		else Manifest.permission.READ_EXTERNAL_STORAGE
	}
}
