package com.example.medpulse.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "medpulse_settings")

data class TimerPreferences(
    val focusDurationMinutes: Int = 50,
    val breakDurationMinutes: Int = 10,
    val autoTransition: Boolean = false,
    val soundVibrateEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val FOCUS_DURATION_MIN = intPreferencesKey("focus_duration_min")
        val BREAK_DURATION_MIN = intPreferencesKey("break_duration_min")
        val AUTO_TRANSITION = booleanPreferencesKey("auto_transition")
        val SOUND_VIBRATE = booleanPreferencesKey("sound_vibrate")
        val LAST_SELECTED_DATE = stringPreferencesKey("last_selected_date")
    }

    val timerPreferencesFlow: Flow<TimerPreferences> = context.dataStore.data.map { prefs ->
        TimerPreferences(
            focusDurationMinutes = prefs[PreferencesKeys.FOCUS_DURATION_MIN] ?: 50,
            breakDurationMinutes = prefs[PreferencesKeys.BREAK_DURATION_MIN] ?: 10,
            autoTransition = prefs[PreferencesKeys.AUTO_TRANSITION] ?: false,
            soundVibrateEnabled = prefs[PreferencesKeys.SOUND_VIBRATE] ?: true
        )
    }

    suspend fun updateTimerSettings(
        focusMin: Int,
        breakMin: Int,
        autoTransition: Boolean,
        soundVibrate: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOCUS_DURATION_MIN] = focusMin.coerceIn(1, 180)
            prefs[PreferencesKeys.BREAK_DURATION_MIN] = breakMin.coerceIn(1, 60)
            prefs[PreferencesKeys.AUTO_TRANSITION] = autoTransition
            prefs[PreferencesKeys.SOUND_VIBRATE] = soundVibrate
        }
    }
}
