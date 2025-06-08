package com.github.dezzk.monjaro_music.app.settings

import android.os.CountDownTimer
import com.github.dezzk.monjaro_music.core.evt.EventBus
import com.github.dezzk.monjaro_music.data.EventSource
import com.github.dezzk.monjaro_music.data.EventType
import com.github.dezzk.monjaro_music.data.SystemEvent

object SleepTimer {

	private var timer: CountDownTimer? = null

	fun start(delay: Int) {
		timer = object : CountDownTimer(delay * 60 * 1000L, 1000) {
			override fun onTick(millisUntilFinished: Long) {
				EventBus.send(SystemEvent(EventSource.SESSION, EventType.SLEEP_TIMER_TICK, (millisUntilFinished / 1000).toInt().toString()))
			}

			override fun onFinish() {
				EventBus.send(SystemEvent(EventSource.SESSION, EventType.SLEEP_TIMER_FINISH))
			}
		}

		timer?.start()
	}

	fun cancel() {
		timer?.cancel()
	}
}