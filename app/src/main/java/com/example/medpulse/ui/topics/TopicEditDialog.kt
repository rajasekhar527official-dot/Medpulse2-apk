package com.example.medpulse.ui.topics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medpulse.data.local.entity.TopicEntity
import com.example.medpulse.data.repository.MedPulseRepository

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicEditDialog(
    initialTopic: TopicEntity? = null,
    defaultDate: String = MedPulseRepository.getTodayDateString(),
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        subject: String,
        targetMinutes: Int,
        priority: String,
        notes: String,
        date: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTopic?.title ?: "") }
    var subject by remember { mutableStateOf(initialTopic?.subject ?: "Cardiology") }
    var targetMinutes by remember { mutableIntStateOf(initialTopic?.targetMinutes ?: 50) }
    var priority by remember { mutableStateOf(initialTopic?.priority ?: "Medium") }
    var notes by remember { mutableStateOf(initialTopic?.notes ?: "") }
    var date by remember { mutableStateOf(initialTopic?.date ?: defaultDate) }

    val commonSubjects = listOf("Cardiology", "Pharmacology", "Pathology", "Anatomy", "Neurology", "Pediatrics", "Surgery", "Internal Medicine")
    val priorities = listOf("High", "Medium", "Low")
    val minutePresets = listOf(25, 40, 50, 60, 90)

    val isTitleValid = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTopic == null) "Add Study Topic" else "Edit Topic",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Topic Title *") },
                    placeholder = { Text("e.g. Cranial Nerves Pathways") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topic_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Medical Category *") },
                    placeholder = { Text("e.g. Anatomy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    commonSubjects.forEach { s ->
                        FilterChip(
                            selected = subject.equals(s, ignoreCase = true),
                            onClick = { subject = s },
                            label = { Text(s, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Target Study Duration: $targetMinutes min",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    minutePresets.forEach { mins ->
                        FilterChip(
                            selected = targetMinutes == mins,
                            onClick = { targetMinutes = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Priority Level",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    priorities.forEach { p ->
                        FilterChip(
                            selected = priority.equals(p, ignoreCase = true),
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Study Goals / High-Yield Notes") },
                    placeholder = { Text("Key concepts, mnemonics, question bank targets...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isTitleValid) {
                        onSave(title, subject, targetMinutes, priority, notes, date)
                    }
                },
                enabled = isTitleValid,
                modifier = Modifier.testTag("save_topic_button")
            ) {
                Text("Save Topic")
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
