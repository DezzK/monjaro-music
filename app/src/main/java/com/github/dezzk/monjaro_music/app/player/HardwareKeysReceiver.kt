package com.github.dezzk.monjaro_music.app.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import com.github.dezzk.monjaro_music.core.evt.EventBus
import com.github.dezzk.monjaro_music.data.EventSource
import com.github.dezzk.monjaro_music.data.EventType
import com.github.dezzk.monjaro_music.data.SystemEvent

const val MB_EVENT = "android.intent.action.MEDIA_BUTTON"

class HardwareKeysReceiver : BroadcastReceiver() {
    private var mainHandler = Handler(Looper.getMainLooper())

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("HardwareKeysReceiver", "Received event: " + intent?.action)
        if (intent?.action == MB_EVENT && intent.hasExtra(Intent.EXTRA_KEY_EVENT) && !PlaybackManager.isPlaying) {
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            if (event?.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                mainHandler.postDelayed({
                    if (!PlaybackManager.isPlaying) {
                        EventBus.send(SystemEvent(EventSource.SESSION, EventType.PLAY))
                    }
                }, 500)
            }
        }
    }
}