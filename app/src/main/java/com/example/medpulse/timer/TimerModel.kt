package com.example.medpulse.timer

enum class TimerMode {
    FOCUS,
    BREAK
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class TimerState(
    val mode: TimerMode = TimerMode.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val totalDurationSeconds: Int = 50 * 60,
    val remainingSeconds: Int = 50 * 60,
    val targetEndTimeEpochMs: Long = 0L,
    val sessionStartEpochMs: Long = 0L,
    val linkedTopicId: Long? = null,
    val linkedTopicTitle: String? = null,
    val customFocusDurationMinutes: Int = 50,
    val customBreakDurationMinutes: Int = 10,
    val autoTransition: Boolean = false,
    val soundVibrateEnabled: Boolean = true
) {
    val progress: Float
        get() = if (totalDurationSeconds > 0) {
            1f - (remainingSeconds.toFloat() / totalDurationSeconds.toFloat())
        } else 0f

    val formattedRemaining: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val isRunning: Boolean get() = status == TimerStatus.RUNNING
    val isPaused: Boolean get() = status == TimerStatus.PAUSED
    val isIdle: Boolean get() = status == TimerStatus.IDLE
}
