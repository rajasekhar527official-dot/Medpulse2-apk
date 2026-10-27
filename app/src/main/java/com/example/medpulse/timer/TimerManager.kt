package com.example.medpulse.timer

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.medpulse.data.local.MedPulseDatabase
import com.example.medpulse.data.preferences.UserPreferencesRepository
import com.example.medpulse.data.repository.MedPulseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class TimerManager private constructor(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "medpulse_timer_engine_prefs"
        private const val KEY_MODE = "timer_mode"
        private const val KEY_STATUS = "timer_status"
        private const val KEY_TOTAL_SEC = "total_sec"
        private const val KEY_REMAINING_SEC = "remaining_sec"
        private const val KEY_TARGET_END_EPOCH_MS = "target_end_epoch_ms"
        private const val KEY_SESSION_START_EPOCH_MS = "session_start_epoch_ms"
        private const val KEY_LINKED_TOPIC_ID = "linked_topic_id"
        private const val KEY_LINKED_TOPIC_TITLE = "linked_topic_title"
        private const val KEY_LAST_RECORDED_KEY = "last_recorded_key"

        @Volatile
        private var INSTANCE: TimerManager? = null

        fun getInstance(context: Context): TimerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimerManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val database = MedPulseDatabase.getInstance(context)
    private val repository = MedPulseRepository(database.topicDao(), database.noteDao(), database.studySessionDao())
    private val preferencesRepository = UserPreferencesRepository(context)

    private val _timerStateFlow = MutableStateFlow(TimerState())
    val timerStateFlow: StateFlow<TimerState> = _timerStateFlow.asStateFlow()

    private var tickerJob: Job? = null
    private var lastRecordedSessionKey: String = prefs.getString(KEY_LAST_RECORDED_KEY, "") ?: ""

    init {
        restorePersistedState()
        // Synchronize with user preferences for default durations
        scope.launch {
            val userPrefs = preferencesRepository.timerPreferencesFlow.first()
            _timerStateFlow.update { current ->
                val focusSec = userPrefs.focusDurationMinutes * 60
                val breakSec = userPrefs.breakDurationMinutes * 60
                val total = if (current.mode == TimerMode.FOCUS) focusSec else breakSec
                val remaining = if (current.status == TimerStatus.IDLE) total else current.remainingSeconds
                current.copy(
                    customFocusDurationMinutes = userPrefs.focusDurationMinutes,
                    customBreakDurationMinutes = userPrefs.breakDurationMinutes,
                    autoTransition = userPrefs.autoTransition,
                    soundVibrateEnabled = userPrefs.soundVibrateEnabled,
                    totalDurationSeconds = if (current.status == TimerStatus.IDLE) total else current.totalDurationSeconds,
                    remainingSeconds = remaining
                )
            }
        }
    }

    private fun restorePersistedState() {
        val modeStr = prefs.getString(KEY_MODE, TimerMode.FOCUS.name) ?: TimerMode.FOCUS.name
        val statusStr = prefs.getString(KEY_STATUS, TimerStatus.IDLE.name) ?: TimerStatus.IDLE.name
        val totalSec = prefs.getInt(KEY_TOTAL_SEC, 50 * 60)
        var remainingSec = prefs.getInt(KEY_REMAINING_SEC, totalSec)
        val targetEndEpochMs = prefs.getLong(KEY_TARGET_END_EPOCH_MS, 0L)
        val sessionStartEpochMs = prefs.getLong(KEY_SESSION_START_EPOCH_MS, 0L)
        val topicId = if (prefs.contains(KEY_LINKED_TOPIC_ID)) prefs.getLong(KEY_LINKED_TOPIC_ID, 0L) else null
        val topicTitle = prefs.getString(KEY_LINKED_TOPIC_TITLE, null)

        val mode = try { TimerMode.valueOf(modeStr) } catch (e: Exception) { TimerMode.FOCUS }
        var status = try { TimerStatus.valueOf(statusStr) } catch (e: Exception) { TimerStatus.IDLE }

        // Process recovery if app was killed while timer was running
        if (status == TimerStatus.RUNNING) {
            val now = System.currentTimeMillis()
            if (targetEndEpochMs > now) {
                remainingSec = ((targetEndEpochMs - now) / 1000L).toInt().coerceAtLeast(1)
            } else {
                // Completed in background/sleep
                remainingSec = 0
                status = TimerStatus.COMPLETED
            }
        }

        _timerStateFlow.value = TimerState(
            mode = mode,
            status = status,
            totalDurationSeconds = totalSec,
            remainingSeconds = remainingSec,
            targetEndTimeEpochMs = targetEndEpochMs,
            sessionStartEpochMs = sessionStartEpochMs,
            linkedTopicId = topicId,
            linkedTopicTitle = topicTitle
        )

        if (status == TimerStatus.RUNNING) {
            startTicker()
        } else if (status == TimerStatus.COMPLETED) {
            scope.launch {
                handleSessionCompletion()
            }
        }
    }

    private fun persistState(state: TimerState) {
        prefs.edit()
            .putString(KEY_MODE, state.mode.name)
            .putString(KEY_STATUS, state.status.name)
            .putInt(KEY_TOTAL_SEC, state.totalDurationSeconds)
            .putInt(KEY_REMAINING_SEC, state.remainingSeconds)
            .putLong(KEY_TARGET_END_EPOCH_MS, state.targetEndTimeEpochMs)
            .putLong(KEY_SESSION_START_EPOCH_MS, state.sessionStartEpochMs)
            .apply {
                if (state.linkedTopicId != null) {
                    putLong(KEY_LINKED_TOPIC_ID, state.linkedTopicId)
                } else {
                    remove(KEY_LINKED_TOPIC_ID)
                }
                putString(KEY_LINKED_TOPIC_TITLE, state.linkedTopicTitle)
            }
            .apply()
    }

    fun startTimer() {
        val current = _timerStateFlow.value
        val now = System.currentTimeMillis()
        val remaining = if (current.remainingSeconds > 0) current.remainingSeconds else current.totalDurationSeconds
        val targetEndTime = now + (remaining * 1000L)
        val startTime = if (current.sessionStartEpochMs > 0L) current.sessionStartEpochMs else now

        val updated = current.copy(
            status = TimerStatus.RUNNING,
            remainingSeconds = remaining,
            targetEndTimeEpochMs = targetEndTime,
            sessionStartEpochMs = startTime
        )
        _timerStateFlow.value = updated
        persistState(updated)

        TimerForegroundService.startService(context, TimerForegroundService.ACTION_START)
        startTicker()
    }

    fun pauseTimer() {
        tickerJob?.cancel()
        val current = _timerStateFlow.value
        if (current.status == TimerStatus.RUNNING) {
            val now = System.currentTimeMillis()
            val remaining = max(0, ((current.targetEndTimeEpochMs - now) / 1000L).toInt())
            val updated = current.copy(
                status = TimerStatus.PAUSED,
                remainingSeconds = remaining
            )
            _timerStateFlow.value = updated
            persistState(updated)
        }
    }

    fun resumeTimer() {
        startTimer()
    }

    fun resetTimer() {
        tickerJob?.cancel()
        val current = _timerStateFlow.value
        val totalSec = when (current.mode) {
            TimerMode.FOCUS -> current.customFocusDurationMinutes * 60
            TimerMode.BREAK -> current.customBreakDurationMinutes * 60
        }
        val updated = current.copy(
            status = TimerStatus.IDLE,
            totalDurationSeconds = totalSec,
            remainingSeconds = totalSec,
            targetEndTimeEpochMs = 0L,
            sessionStartEpochMs = 0L
        )
        _timerStateFlow.value = updated
        persistState(updated)
        TimerForegroundService.stopService(context)
    }

    fun skipSession() {
        tickerJob?.cancel()
        val current = _timerStateFlow.value
        val nextMode = if (current.mode == TimerMode.FOCUS) TimerMode.BREAK else TimerMode.FOCUS
        val nextTotalSec = when (nextMode) {
            TimerMode.FOCUS -> current.customFocusDurationMinutes * 60
            TimerMode.BREAK -> current.customBreakDurationMinutes * 60
        }
        val updated = current.copy(
            mode = nextMode,
            status = TimerStatus.IDLE,
            totalDurationSeconds = nextTotalSec,
            remainingSeconds = nextTotalSec,
            targetEndTimeEpochMs = 0L,
            sessionStartEpochMs = 0L
        )
        _timerStateFlow.value = updated
        persistState(updated)
        TimerForegroundService.stopService(context)
    }

    fun linkTopic(topicId: Long?, topicTitle: String?) {
        _timerStateFlow.update { it.copy(linkedTopicId = topicId, linkedTopicTitle = topicTitle) }
        persistState(_timerStateFlow.value)
    }

    fun updateSettings(focusMin: Int, breakMin: Int, autoTransition: Boolean, soundVibrate: Boolean) {
        scope.launch {
            preferencesRepository.updateTimerSettings(focusMin, breakMin, autoTransition, soundVibrate)
        }
        _timerStateFlow.update { current ->
            val isFocus = current.mode == TimerMode.FOCUS
            val newTotal = if (isFocus) focusMin * 60 else breakMin * 60
            val newRemaining = if (current.status == TimerStatus.IDLE) newTotal else current.remainingSeconds
            current.copy(
                customFocusDurationMinutes = focusMin,
                customBreakDurationMinutes = breakMin,
                autoTransition = autoTransition,
                soundVibrateEnabled = soundVibrate,
                totalDurationSeconds = if (current.status == TimerStatus.IDLE) newTotal else current.totalDurationSeconds,
                remainingSeconds = newRemaining
            )
        }
        persistState(_timerStateFlow.value)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                delay(500L)
                val current = _timerStateFlow.value
                if (current.status != TimerStatus.RUNNING) break

                val now = System.currentTimeMillis()
                val leftSec = max(0, ((current.targetEndTimeEpochMs - now + 999) / 1000L).toInt())

                if (leftSec <= 0) {
                    _timerStateFlow.update { it.copy(remainingSeconds = 0, status = TimerStatus.COMPLETED) }
                    persistState(_timerStateFlow.value)
                    handleSessionCompletion()
                    break
                } else {
                    _timerStateFlow.update { it.copy(remainingSeconds = leftSec) }
                    // Update SharedPreferences every 5 seconds to reduce I/O while keeping recovery fresh
                    if (leftSec % 5 == 0) {
                        persistState(_timerStateFlow.value)
                    }
                }
            }
        }
    }

    private suspend fun handleSessionCompletion() {
        val completedState = _timerStateFlow.value
        val sessionKey = "${completedState.sessionStartEpochMs}_${completedState.mode}"

        // Prevent duplicate recording of the exact same session
        if (sessionKey != lastRecordedSessionKey && completedState.sessionStartEpochMs > 0L) {
            lastRecordedSessionKey = sessionKey
            prefs.edit().putString(KEY_LAST_RECORDED_KEY, sessionKey).apply()

            val studiedSeconds = completedState.totalDurationSeconds
            repository.recordSession(
                sessionType = completedState.mode.name,
                durationSeconds = studiedSeconds,
                startedAtEpochMs = completedState.sessionStartEpochMs,
                endedAtEpochMs = System.currentTimeMillis(),
                linkedTopicId = completedState.linkedTopicId,
                linkedTopicTitle = completedState.linkedTopicTitle
            )
        }

        // Trigger haptic vibration if enabled
        if (completedState.soundVibrateEnabled) {
            triggerVibration()
        }

        val nextMode = if (completedState.mode == TimerMode.FOCUS) TimerMode.BREAK else TimerMode.FOCUS
        val nextTotalSec = when (nextMode) {
            TimerMode.FOCUS -> completedState.customFocusDurationMinutes * 60
            TimerMode.BREAK -> completedState.customBreakDurationMinutes * 60
        }

        if (completedState.autoTransition) {
            // Automatically start the next session
            val now = System.currentTimeMillis()
            val nextState = completedState.copy(
                mode = nextMode,
                status = TimerStatus.RUNNING,
                totalDurationSeconds = nextTotalSec,
                remainingSeconds = nextTotalSec,
                targetEndTimeEpochMs = now + (nextTotalSec * 1000L),
                sessionStartEpochMs = now
            )
            _timerStateFlow.value = nextState
            persistState(nextState)
            startTicker()
        } else {
            // Idle in the next mode, ready for user
            val nextState = completedState.copy(
                mode = nextMode,
                status = TimerStatus.IDLE,
                totalDurationSeconds = nextTotalSec,
                remainingSeconds = nextTotalSec,
                targetEndTimeEpochMs = 0L,
                sessionStartEpochMs = 0L
            )
            _timerStateFlow.value = nextState
            persistState(nextState)
            TimerForegroundService.stopService(context)
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
                )
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
                    )
                } else {
                    vibrator?.vibrate(longArrayOf(0, 300, 200, 300), -1)
                }
            }
        } catch (_: Exception) {}
    }
}
