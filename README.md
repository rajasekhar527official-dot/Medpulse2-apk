# MedPulse Study — Native Android Study Productivity Application

**MedPulse Study** is a high-performance native Android application engineered for rigorous medical and graduate study sessions. It combines a persistent Pomodoro-style study timer backed by an Android Foreground Service, a dedicated study notes system inspired by Samsung Notes and Samsung One UI, a daily medical topic planner with checklist persistence, and permanent study history & analytics.

---

## 🚀 GitHub Actions: Automated APK Building & Releases

This repository is pre-configured with a full GitHub Actions CI/CD workflow (`.github/workflows/build-apk.yml`) that automatically tests, compiles, packages, and releases installable debug APKs.

### How it Works:
1. **Push to Main/Master:** Every push or merge to the `main` or `master` branch automatically runs the workflow, executes automated tests, compiles `MedPulseStudy-debug.apk`, and creates a new **GitHub Release** with the APK attached for one-click downloading.
2. **Git Tag Releases:** Pushing any version tag (e.g. `git tag v1.0.0 && git push origin v1.0.0`) generates a formal versioned release.
3. **Manual Trigger (Workflow Dispatch):**
   - Go to your repository on GitHub.
   - Click the **Actions** tab.
   - Select **"Build and Release MedPulse Study APK"**.
   - Click **Run workflow** -> **Run workflow**.
4. **Direct Download from GitHub:**
   - **From Releases:** Go to `https://github.com/<your-username>/<repo>/releases` and download `MedPulseStudy-debug.apk`.
   - **From Actions Artifacts:** Click any finished run in the **Actions** tab and download the `MedPulseStudy-debug-apk` zip containing the APK.

---

## Architecture & Technology Stack

- **Language:** 100% Kotlin with Coroutines & Reactive Flow
- **UI Framework:** Jetpack Compose with Material Design 3 (Dynamic / Clinical Slate & MedTeal palette)
- **Local Persistence:** Android Jetpack Room Database (SQLite with KSP compiler, Room DAOs, and transactions)
- **Timer Engine & Background Operation:** Android Foreground Service (`TimerForegroundService`) with ongoing status notification, interactive notification actions (Pause, Resume, Reset, Skip), monotonic elapsed-time calculations, and state persistence across process deaths and device restarts.
- **Preferences:** Jetpack DataStore Preferences for user configurations (custom focus minutes, break minutes, auto-transition, sound/vibrations)
- **Notifications:** Android Notification Manager APIs with Android 13+ runtime permission request (`POST_NOTIFICATIONS`) and notification channel configuration.
- **Testing:** Local JVM unit and Robolectric tests for timer logic, session aggregation, and Room database operations.

---

## Key Features

### 1. Study Timer
- **Default Durations:** 50-minute Focus session, 10-minute Break session (customizable to 25m, 45m, 60m, 90m, or custom durations).
- **Controls:** Start, Pause, Resume, Reset, Skip to Break, and Skip Break.
- **Topic Linkage:** Link any focus session to a specific study topic (e.g. *Cardiovascular Pharmacology*), attributing study time directly to that subject.
- **Auto-Transition:** Optional toggle to automatically advance from focus to break and vice versa.
- **Session Idempotency:** Guarantees no duplicate session recordings upon completion or process restart.

### 2. Foreground Service & Notification
- Runs an active `ForegroundService` when a countdown is active.
- Persistent notification displaying remaining time (`MM:SS`), session mode (`Focus` vs `Break`), and linked topic.
- Working notification action buttons to Pause, Resume, Skip, or Stop directly from the lock screen or status bar.
- Survives activity destruction and Recents clearance.

### 3. Study Notes (Samsung Notes Inspired)
- Dedicated notes interface featuring Samsung One UI pastel card palettes (White, Lemon, Peach, Rose, Mint, Sky, Lavender).
- Pin important notes to the top of the feed.
- Full-text search across note titles, body text, medical subjects, and hashtag tags.
- Direct linking between notes and planned study topics.
- Auto-save debounce engine ensures no study thoughts or question bank pearls are lost.
- Quick markdown formatting shortcuts (bullets, bold, italics, high-yield badges).

### 4. Daily Topic Planner
- Daily checklist with interactive checkboxes that immediately persist completion status and timestamps.
- Priority indicators (High, Medium, Low) and custom medical categories (Cardiology, Anatomy, Pharmacology, Pathology, etc.).
- Target study duration and goal notes per topic.
- One-tap "Start Timer" button beside any topic to instantly launch a study session.
- Date navigator to review or plan for past and future dates.

### 5. Permanent History & Analytics
- Complete study session log with duration, start/end timestamps, and topic attribution.
- Total study hours and completed session counters.
- Topic archive and "Study Today" reuse button to reschedule previous topics onto today's planner.
- Fully offline and local-first.

---

## Project Structure

```
.github/
└── workflows/
    └── build-apk.yml                    # Automated GitHub Actions workflow for APK compilation & releases
app/src/main/
├── AndroidManifest.xml                  # Permissions, Application, and Foreground Service declarations
├── java/com/example/
│   ├── MainActivity.kt                  # Edge-to-edge Compose container, navigation, permission launcher
│   ├── MedPulseApp.kt                   # Application class, notification channels, initial seed data
│   ├── ui/
│   │   ├── theme/                       # Color.kt, Theme.kt, Type.kt (Clinical slate & teal M3 theme)
│   │   ├── navigation/                  # NavDestination.kt (Screen routes and navigation items)
│   │   ├── components/                  # CommonComponents.kt (Color palettes, badges, chips)
│   │   ├── dashboard/                   # DashboardScreen.kt (Pulse timer card, stats, checklist preview)
│   │   ├── topics/                      # TopicsScreen.kt, TopicEditDialog.kt (Daily planner & dialogs)
│   │   ├── notes/                       # NotesScreen.kt, NoteEditScreen.kt (Samsung Notes UI)
│   │   ├── history/                     # HistoryScreen.kt (Session logs, archive, reuse)
│   │   └── timer/                       # TimerSettingsDialog.kt (Custom durations & auto-transition)
│   ├── timer/
│   │   ├── TimerModel.kt                # TimerState, TimerMode, TimerStatus
│   │   ├── TimerForegroundService.kt    # Android Foreground Service & ongoing notifications
│   │   └── TimerManager.kt              # Monotonic countdown engine & state recovery
│   └── data/
│       ├── local/
│       │   ├── MedPulseDatabase.kt      # Room Database
│       │   ├── entity/                  # TopicEntity, NoteEntity, StudySessionEntity
│       │   └── dao/                     # TopicDao, NoteDao, StudySessionDao
│       ├── preferences/                 # UserPreferencesRepository (DataStore)
│       └── repository/                  # MedPulseRepository (Unified business data layer)
```

---

## Local Development & CLI Commands

### 1. Build the Debug APK locally
```bash
./gradlew :app:assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Run Automated Tests locally
```bash
./gradlew :app:testDebugUnitTest
```

### 3. Install on Connected Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
