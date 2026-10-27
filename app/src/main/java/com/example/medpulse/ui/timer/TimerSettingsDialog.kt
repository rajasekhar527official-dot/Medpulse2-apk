package com.example.medpulse.ui.timer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerSettingsDialog(
    initialFocusMin: Int,
    initialBreakMin: Int,
    initialAutoTransition: Boolean,
    initialSoundVibrate: Boolean,
    onDismiss: () -> Unit,
    onSave: (focusMin: Int, breakMin: Int, autoTransition: Boolean, soundVibrate: Boolean) -> Unit
) {
    var focusMin by remember { mutableIntStateOf(initialFocusMin) }
    var breakMin by remember { mutableIntStateOf(initialBreakMin) }
    var autoTransition by remember { mutableStateOf(initialAutoTransition) }
    var soundVibrate by remember { mutableStateOf(initialSoundVibrate) }

    val focusPresets = listOf(25, 45, 50, 60, 90)
    val breakPresets = listOf(5, 10, 15, 20)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Timer Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Focus Duration
                Text(
                    text = "Focus Duration: $focusMin min",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    focusPresets.forEach { preset ->
                        FilterChip(
                            selected = focusMin == preset,
                            onClick = { focusMin = preset },
                            label = { Text("${preset}m") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Break Duration
                Text(
                    text = "Break Duration: $breakMin min",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    breakPresets.forEach { preset ->
                        FilterChip(
                            selected = breakMin == preset,
                            onClick = { breakMin = preset },
                            label = { Text("${preset}m") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Auto transition toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { autoTransition = !autoTransition },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Start Next Session",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Automatically transition between focus and break",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoTransition,
                        onCheckedChange = { autoTransition = it },
                        modifier = Modifier.testTag("auto_transition_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sound & Vibration toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { soundVibrate = !soundVibrate },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vibration & Alerts",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Pulse alert when a countdown finishes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = soundVibrate,
                        onCheckedChange = { soundVibrate = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(focusMin, breakMin, autoTransition, soundVibrate)
                },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
