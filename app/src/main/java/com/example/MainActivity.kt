package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medpulse.data.local.entity.NoteEntity
import com.example.medpulse.data.local.entity.TopicEntity
import com.example.medpulse.data.repository.MedPulseRepository
import com.example.medpulse.timer.TimerManager
import com.example.medpulse.ui.dashboard.DashboardScreen
import com.example.medpulse.ui.history.HistoryScreen
import com.example.medpulse.ui.navigation.Screen
import com.example.medpulse.ui.notes.NoteEditScreen
import com.example.medpulse.ui.notes.NotesScreen
import com.example.medpulse.ui.timer.TimerSettingsDialog
import com.example.medpulse.ui.topics.TopicEditDialog
import com.example.medpulse.ui.topics.TopicsScreen
import com.example.ui.theme.MedPulseStudyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MedPulseStudyTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val app = context.applicationContext as MedPulseApp
    val repository = app.repository
    val timerManager = remember { TimerManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    // Notification Permission Handling for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied handled gracefully */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Navigation and Modal States
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isCreatingNewNote by remember { mutableStateOf(false) }
    var showTimerSettings by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var editingTopic by remember { mutableStateOf<TopicEntity?>(null) }
    var notesSearchQuery by remember { mutableStateOf("") }

    val todayDate = remember { MedPulseRepository.getTodayDateString() }
    var selectedTopicsDate by remember { mutableStateOf(todayDate) }

    // Live States
    val timerState by timerManager.timerStateFlow.collectAsStateWithLifecycle()
    val todayTopics by repository.getAllTopicsForDate(todayDate).collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedDateTopics by repository.getAllTopicsForDate(selectedTopicsDate).collectAsStateWithLifecycle(initialValue = emptyList())
    val allTopics by repository.getAllTopics().collectAsStateWithLifecycle(initialValue = emptyList())
    val archivedTopics by repository.getArchivedTopics().collectAsStateWithLifecycle(initialValue = emptyList())
    val allNotes by repository.getAllNotes().collectAsStateWithLifecycle(initialValue = emptyList())
    val allSessions by repository.getAllSessions().collectAsStateWithLifecycle(initialValue = emptyList())
    val todayFocusSeconds by repository.getTotalFocusSecondsForDate(todayDate).collectAsStateWithLifecycle(initialValue = 0)
    val todaySessionCount by repository.getFocusSessionCountForDate(todayDate).collectAsStateWithLifecycle(initialValue = 0)

    // Note Editor Screen Overlay
    if (isCreatingNewNote || editingNote != null) {
        NoteEditScreen(
            note = editingNote,
            availableTopics = allTopics,
            onSaveNote = { noteToSave ->
                scope.launch {
                    val savedId = repository.saveNote(
                        id = noteToSave.id,
                        title = noteToSave.title,
                        body = noteToSave.body,
                        subject = noteToSave.subject,
                        tags = noteToSave.tags,
                        linkedTopicId = noteToSave.linkedTopicId,
                        linkedTopicTitle = noteToSave.linkedTopicTitle,
                        colorIndex = noteToSave.colorIndex,
                        isPinned = noteToSave.isPinned
                    )
                    if (editingNote == null) {
                        editingNote = noteToSave.copy(id = savedId)
                    }
                }
            },
            onDeleteNote = { noteToDelete ->
                scope.launch {
                    repository.deleteNote(noteToDelete)
                }
                editingNote = null
                isCreatingNewNote = false
            },
            onNavigateBack = {
                editingNote = null
                isCreatingNewNote = false
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                Screen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            Screen.HOME -> {
                DashboardScreen(
                    timerState = timerState,
                    todayTopics = todayTopics,
                    todayTotalFocusSeconds = todayFocusSeconds,
                    todaySessionCount = todaySessionCount,
                    onStartTimer = { timerManager.startTimer() },
                    onPauseTimer = { timerManager.pauseTimer() },
                    onResumeTimer = { timerManager.resumeTimer() },
                    onResetTimer = { timerManager.resetTimer() },
                    onSkipSession = { timerManager.skipSession() },
                    onOpenTimerSettings = { showTimerSettings = true },
                    onToggleTopicCompletion = { topic ->
                        scope.launch { repository.toggleTopicCompletion(topic) }
                    },
                    onAddNewTopicClick = { showAddTopicDialog = true },
                    onQuickNoteClick = { isCreatingNewNote = true },
                    onNavigateToTopics = { currentScreen = Screen.TOPICS },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.TOPICS -> {
                TopicsScreen(
                    topics = selectedDateTopics,
                    selectedDate = selectedTopicsDate,
                    onDateChange = { selectedTopicsDate = it },
                    onToggleCompletion = { topic ->
                        scope.launch { repository.toggleTopicCompletion(topic) }
                    },
                    onEditTopic = { topic -> editingTopic = topic },
                    onArchiveTopic = { topic ->
                        scope.launch { repository.archiveTopic(topic) }
                    },
                    onDeleteTopic = { topic ->
                        scope.launch { repository.deleteTopic(topic) }
                    },
                    onStartTimerWithTopic = { topic ->
                        timerManager.linkTopic(topic.id, topic.title)
                        currentScreen = Screen.HOME
                        timerManager.startTimer()
                    },
                    onAddTopicClick = { showAddTopicDialog = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.NOTES -> {
                NotesScreen(
                    notes = allNotes,
                    searchQuery = notesSearchQuery,
                    onSearchQueryChange = { notesSearchQuery = it },
                    onNoteClick = { note -> editingNote = note },
                    onNewNoteClick = { isCreatingNewNote = true },
                    onTogglePin = { note ->
                        scope.launch { repository.toggleNotePin(note) }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.HISTORY -> {
                HistoryScreen(
                    sessions = allSessions,
                    allTopics = allTopics,
                    archivedTopics = archivedTopics,
                    onReuseTopicForToday = { topic ->
                        scope.launch {
                            repository.reuseTopicForDate(topic, todayDate)
                            currentScreen = Screen.TOPICS
                            selectedTopicsDate = todayDate
                        }
                    },
                    onUnarchiveTopic = { topic ->
                        scope.launch { repository.unarchiveTopic(topic) }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Dialogs
    if (showTimerSettings) {
        TimerSettingsDialog(
            initialFocusMin = timerState.customFocusDurationMinutes,
            initialBreakMin = timerState.customBreakDurationMinutes,
            initialAutoTransition = timerState.autoTransition,
            initialSoundVibrate = timerState.soundVibrateEnabled,
            onDismiss = { showTimerSettings = false },
            onSave = { focusMin, breakMin, autoTransition, soundVibrate ->
                timerManager.updateSettings(focusMin, breakMin, autoTransition, soundVibrate)
                showTimerSettings = false
            }
        )
    }

    if (showAddTopicDialog) {
        TopicEditDialog(
            initialTopic = null,
            defaultDate = selectedTopicsDate,
            onDismiss = { showAddTopicDialog = false },
            onSave = { title, subject, targetMinutes, priority, notes, date ->
                scope.launch {
                    repository.addTopic(
                        title = title,
                        subject = subject,
                        targetMinutes = targetMinutes,
                        priority = priority,
                        notes = notes,
                        date = date
                    )
                }
                showAddTopicDialog = false
            }
        )
    }

    if (editingTopic != null) {
        TopicEditDialog(
            initialTopic = editingTopic,
            defaultDate = editingTopic?.date ?: todayDate,
            onDismiss = { editingTopic = null },
            onSave = { title, subject, targetMinutes, priority, notes, date ->
                editingTopic?.let { existing ->
                    scope.launch {
                        repository.updateTopic(
                            existing.copy(
                                title = title,
                                subject = subject,
                                targetMinutes = targetMinutes,
                                priority = priority,
                                notes = notes,
                                date = date
                            )
                        )
                    }
                }
                editingTopic = null
            }
        )
    }
}
