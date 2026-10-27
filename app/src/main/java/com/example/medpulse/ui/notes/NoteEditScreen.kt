package com.example.medpulse.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medpulse.data.local.entity.NoteEntity
import com.example.medpulse.data.local.entity.TopicEntity
import com.example.medpulse.ui.components.ColorPickerRow
import com.example.medpulse.ui.components.NotePalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditScreen(
    note: NoteEntity?,
    availableTopics: List<TopicEntity>,
    onSaveNote: (NoteEntity) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    var noteId by remember { mutableStateOf(note?.id ?: 0L) }
    var title by remember { mutableStateOf(note?.title ?: "") }
    var body by remember { mutableStateOf(note?.body ?: "") }
    var subject by remember { mutableStateOf(note?.subject ?: "") }
    var tags by remember { mutableStateOf(note?.tags ?: "") }
    var linkedTopicId by remember { mutableStateOf(note?.linkedTopicId) }
    var linkedTopicTitle by remember { mutableStateOf(note?.linkedTopicTitle) }
    var colorIndex by remember { mutableIntStateOf(note?.colorIndex ?: 0) }
    var isPinned by remember { mutableStateOf(note?.isPinned ?: false) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showTopicMenu by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf("Saved") }

    fun buildCurrentNote(): NoteEntity {
        return NoteEntity(
            id = noteId,
            title = title,
            body = body,
            subject = subject,
            tags = tags,
            linkedTopicId = linkedTopicId,
            linkedTopicTitle = linkedTopicTitle,
            colorIndex = colorIndex,
            isPinned = isPinned,
            createdAtEpochMs = note?.createdAtEpochMs ?: System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    // Auto-save debounce effect
    LaunchedEffect(title, body, subject, tags, linkedTopicId, colorIndex, isPinned) {
        saveStatus = "Editing..."
        delay(800L)
        if (title.isNotBlank() || body.isNotBlank()) {
            val toSave = buildCurrentNote()
            onSaveNote(toSave)
            saveStatus = "Saved"
        }
    }

    // Ensure save on back
    BackHandler {
        if (title.isNotBlank() || body.isNotBlank()) {
            onSaveNote(buildCurrentNote())
        }
        onNavigateBack()
    }

    val noteBg = NotePalette.getColor(colorIndex, isDark)
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val subtextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(noteBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Samsung Notes One UI top bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = if (noteId == 0L) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = saveStatus,
                        fontSize = 11.sp,
                        color = subtextColor
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (title.isNotBlank() || body.isNotBlank()) {
                            onSaveNote(buildCurrentNote())
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("note_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
            },
            actions = {
                // Pin toggle
                IconButton(onClick = { isPinned = !isPinned }) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin Note",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else textColor
                    )
                }

                // Palette toggle
                IconButton(onClick = { showColorPicker = !showColorPicker }) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Card Color",
                        tint = textColor
                    )
                }

                // Delete
                if (noteId != 0L) {
                    IconButton(onClick = {
                        note?.let { onDeleteNote(it) }
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = textColor
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Color Picker bar if opened
        if (showColorPicker) {
            Surface(
                color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Samsung Notes Pastel Backgrounds",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ColorPickerRow(
                        selectedIndex = colorIndex,
                        isDark = isDark,
                        onSelectIndex = {
                            colorIndex = it
                            showColorPicker = false
                        }
                    )
                }
            }
        }

        // Quick toolbar for medical study notes formatting & topic linkage
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Link to Study Topic chip
            Box {
                Surface(
                    onClick = { showTopicMenu = true },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Topic",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = linkedTopicTitle ?: "Link Topic",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = showTopicMenu,
                    onDismissRequest = { showTopicMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None (Unlinked)") },
                        onClick = {
                            linkedTopicId = null
                            linkedTopicTitle = null
                            showTopicMenu = false
                        }
                    )
                    availableTopics.forEach { topic ->
                        DropdownMenuItem(
                            text = { Text("${topic.subject}: ${topic.title}") },
                            onClick = {
                                linkedTopicId = topic.id
                                linkedTopicTitle = topic.title
                                showTopicMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Markdown / Medical symbols
            FilledTonalIconButton(
                onClick = { body = "$body\n• " },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet", modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            FilledTonalIconButton(
                onClick = { body = "$body**Important:** " },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            FilledTonalIconButton(
                onClick = { body = "$body*Pearl:* " },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                onClick = { body = "$body[High-Yield] " },
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "HY+",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        // Meta tags & subject inputs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                placeholder = { Text("Subject (e.g. Pharmacology)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                textStyle = TextStyle(fontSize = 12.sp, color = textColor)
            )

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                placeholder = { Text("Tags (comma-separated)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                textStyle = TextStyle(fontSize = 12.sp, color = textColor)
            )
        }

        // Title field (Clean One UI title style)
        BasicTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .testTag("note_title_input"),
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontFamily = FontFamily.Default
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                if (title.isEmpty()) {
                    Text(
                        text = "Title",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = subtextColor.copy(alpha = 0.6f)
                    )
                }
                innerTextField()
            }
        )

        // Body field (spacious, auto-expanding, scrollable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            BasicTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("note_body_input"),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = textColor,
                    fontFamily = FontFamily.Default
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (body.isEmpty()) {
                        Text(
                            text = "Start taking study notes, clinical pearls, mnemonics, or question bank explanations...",
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = subtextColor.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Bottom stats bar (Word count, date)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val wordCount = body.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
            Text(
                text = "$wordCount words • ${body.length} characters",
                fontSize = 11.sp,
                color = subtextColor
            )
            val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())
            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = subtextColor
            )
        }
    }
}
