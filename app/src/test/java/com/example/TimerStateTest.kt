package com.example

import com.example.medpulse.timer.TimerMode
import com.example.medpulse.timer.TimerState
import com.example.medpulse.timer.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {

    @Test
    fun testDefaultTimerState() {
        val state = TimerState()
        assertEquals(TimerMode.FOCUS, state.mode)
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(50 * 60, state.totalDurationSeconds)
        assertEquals(50 * 60, state.remainingSeconds)
        assertEquals("50:00", state.formattedRemaining)
        assertEquals(0f, state.progress, 0.001f)
        assertTrue(state.isIdle)
        assertFalse(state.isRunning)
    }

    @Test
    fun testProgressAndFormattingCalculations() {
        val state = TimerState(
            totalDurationSeconds = 3000,
            remainingSeconds = 1500, // half elapsed
            status = TimerStatus.RUNNING
        )
        assertEquals("25:00", state.formattedRemaining)
        assertEquals(0.5f, state.progress, 0.001f)
        assertTrue(state.isRunning)
        assertFalse(state.isPaused)
    }

    @Test
    fun testBreakStateFormatting() {
        val breakState = TimerState(
            mode = TimerMode.BREAK,
            totalDurationSeconds = 600,
            remainingSeconds = 450,
            status = TimerStatus.RUNNING
        )
        assertEquals(TimerMode.BREAK, breakState.mode)
        assertEquals("07:30", breakState.formattedRemaining)
        assertEquals(0.25f, breakState.progress, 0.001f)
    }

    @Test
    fun testCompletedProgress() {
        val completedState = TimerState(
            totalDurationSeconds = 3000,
            remainingSeconds = 0,
            status = TimerStatus.COMPLETED
        )
        assertEquals("00:00", completedState.formattedRemaining)
        assertEquals(1.0f, completedState.progress, 0.001f)
    }
}
