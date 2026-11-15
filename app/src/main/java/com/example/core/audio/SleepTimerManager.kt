package com.example.core.audio

import com.example.domain.models.SleepTimerMode
import com.example.domain.models.SleepTimerState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SleepTimerManager(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val onTimerFinished: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var countdownJob: Job? = null

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    fun startDurationTimer(minutes: Int) {
        val totalMs = minutes.coerceAtLeast(1) * 60_000L
        val targetTime = Date(System.currentTimeMillis() + totalMs)
        val label = SimpleDateFormat("HH:mm", Locale.getDefault()).format(targetTime)
        startCountdownMs(totalMs, label)
    }

    fun startUntilClockTime(hour24: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val diffMs = (target.timeInMillis - now.timeInMillis).coerceAtLeast(1000L)
        val label = String.format(Locale.getDefault(), "%02d:%02d", hour24, minute)
        startCountdownMs(diffMs, label)
    }

    private fun startCountdownMs(totalMs: Long, label: String) {
        countdownJob?.cancel()
        _state.value = SleepTimerState(
            mode = SleepTimerMode.DURATION_OR_TIME,
            remainingMs = totalMs,
            totalMs = totalMs,
            isPaused = false,
            targetClockLabel = label,
        )
        launchTicker()
    }

    fun startTrackCountTimer(trackCount: Int) {
        countdownJob?.cancel()
        val count = trackCount.coerceAtLeast(1)
        _state.value = SleepTimerState(
            mode = SleepTimerMode.TRACK_COUNT,
            remainingTracks = count,
            targetTracks = count,
            isPaused = false,
        )
    }

    fun onTrackTransitioned() {
        val current = _state.value
        if (current.mode != SleepTimerMode.TRACK_COUNT || current.isPaused) return
        val nextRemaining = current.remainingTracks - 1
        if (nextRemaining <= 0) {
            cancel()
            onTimerFinished()
        } else {
            _state.value = current.copy(remainingTracks = nextRemaining)
        }
    }

    fun pauseOrResume() {
        val current = _state.value
        if (current.mode == SleepTimerMode.NONE) return
        val newPaused = !current.isPaused
        _state.value = current.copy(isPaused = newPaused)
        if (!newPaused && current.mode == SleepTimerMode.DURATION_OR_TIME) {
            launchTicker()
        } else if (newPaused) {
            countdownJob?.cancel()
        }
    }

    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
        _state.value = SleepTimerState()
    }

    private fun launchTicker() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (_state.value.mode == SleepTimerMode.DURATION_OR_TIME && !_state.value.isPaused) {
                delay(1000L)
                val rem = _state.value.remainingMs - 1000L
                if (rem <= 0L) {
                    cancel()
                    onTimerFinished()
                    break
                } else {
                    _state.value = _state.value.copy(remainingMs = rem)
                }
            }
        }
    }
}
