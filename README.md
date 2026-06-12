# Aura — AI Personal Assistant
### Android · Kotlin · Jetpack Compose · Clean Architecture

> *"A fully on-device voice + text AI assistant with a personality engine, real-time audio visualisation, and a production-grade offline-first data layer."*

---

## What This Project Demonstrates

This is a complete Android application built to production standards, showcasing the full breadth of modern Android engineering — from low-level `AudioRecord` PCM amplitude capture to a Hilt-wired Clean Architecture with WorkManager sync, Room pagination, and animated Canvas UI.

It was built as a technical assignment and intentionally covers every layer of the stack a senior Android engineer is expected to own.

---

## Feature Overview

| Feature | Technology |
|---|---|
| Offline AI responses | Local rule engine + `ResponseStyleGenerator` |
| Voice input | `SpeechRecognizer` + `AudioRecord` amplitude |
| Animated AI avatar | Pure Canvas API — no Lottie, no GIF |
| Personality engine | 12-trait system that rewrites AI responses |
| Smart reminders | Natural language parsing + `AlarmManager` exact alarms |
| Chat history | Room + Paging 3 (20 messages per page) |
| Background sync | WorkManager with network constraint + exponential backoff |
| Swipeable onboarding | `HorizontalPager` with validation-gated swipe |
| Data persistence | Room DB + DataStore Preferences |
| Dependency injection | Hilt across all layers |

---

## Architecture

```
app/
├── presentation/          # Compose UI + ViewModels (MVVM)
│   ├── home/              # HomeScreen, HomeViewModel
│   ├── chat/              # ChatScreen, ChatViewModel
│   ├── reminders/         # RemindersScreen, RemindersViewModel
│   └── onboarding/        # OnboardingScreen, OnboardingViewModel
│
├── domain/                # Pure Kotlin — no Android imports
│   ├── models/            # UserProfile, ChatMessage, Reminder, AssistantState…
│   ├── repositories/      # Interfaces only
│   └── usecases/          # SendMessageUseCase, CreateReminderUseCase
│
├── data/                  # Implementation details
│   ├── room/              # AuraDatabase, DAOs, Entities, TypeConverters
│   ├── datastore/         # UserProfileDataStore, SyncPreferences
│   ├── repository/        # Concrete repository implementations
│   └── sync/              # SyncWorker, SyncManager
│
├── audio/                 # SpeechRecognizerManager, AudioRecorderManager
├── notifications/         # ReminderScheduler, ReminderAlarmReceiver
├── navigation/            # AuraNavGraph, Screen sealed class
└── di/                    # Hilt modules
```

The domain layer has **zero Android imports** — it can be unit tested with plain JVM. ViewModels depend on domain interfaces only; data implementations are swapped in by Hilt at runtime.

---

## Key Engineering Decisions

### 1. Clean Architecture with Strict Layer Boundaries

The project enforces a hard rule: the domain layer references nothing from `android.*`. Use cases receive `StateFlow` references rather than Android contexts. This makes the coroutine state machine fully testable without Robolectric.

*Why it matters:* Recruiters frequently ask "how would you unit test this?" — the answer here is "spin up a plain JVM test, inject a fake repository, collect the StateFlow."

---

### 2. Coroutine State Machine via `sealed class` + `StateFlow`

Every user message passes through a 6-state pipeline:

```
Typing → Validating → Processing → Responding → Idle
                                       ↓ (timeout)
                                      Error  ←──────┐
                                       └── retry ───┘
```

Implemented in `SendMessageUseCase` with `withTimeout(8_000ms)`. If the user sends a new message mid-flight, `pipelineJob?.cancel()` tears down the previous coroutine cleanly before relaunching. Each state drives a distinct UI in both `HomeScreen` (AuraCircle colour/animation) and `ChatScreen` (top-bar label + colour).

*Decision:* `StateFlow` over `LiveData` — it's coroutine-native, has a guaranteed initial value, and doesn't require lifecycle observers in the ViewModel.

---

### 3. AuraCircle — Pure Canvas Animation

The visual centrepiece is drawn entirely in Compose's `DrawScope` — no Lottie, no GIF, no vector animator. It has four concentric rendering layers:

```
Layer 4 → Far outer glow      (listening + amplitude only)
Layer 3 → Mid glow halo       (3-stop radial gradient)
Layer 2 → Inner glow bloom    (brighter, tighter)
Layer 1 → Core solid circle   (glow-centre → core-edge gradient)
         + Spinning arcs       (Processing / Responding states)
         + Pulse rings         (Listening — radius reacts to mic amplitude)
```

`AudioRecord` PCM samples are RMS-normalised to `[0, 1]` every 60ms and piped through `animateFloatAsState(spring(dampingRatio = 0.55f))` before reaching the canvas — so the circle breathes with the voice rather than jittering.

Amplitude 0→1 also shifts the listening colour from **blue → cyan → violet → fuchsia → pink**, giving a live "energy meter" effect.

*Decision:* Canvas over Lottie because the animation must react to real-time data (mic amplitude). Pre-baked Lottie files cannot be driven by external float values without a custom property mapping that would be more complex than raw Canvas.

---

### 4. Offline-First Data Layer

```
User action
    │
    ▼
Room DB (source of truth) ──→ UI via Flow
    │
    ▼ (on network available)
WorkManager SyncWorker
    │   • Queries isSynced = 0 rows
    │   • Pushes delta only (lastSyncedAt timestamp)
    │   • On conflict: local wins
    │   • Exponential backoff on failure
    ▼
Remote backend (stub — ready for implementation)
```

`SyncStatus` (`Idle / Syncing / Synced / Failed`) is a `StateFlow` in `SyncManager`, observed directly in `HomeScreen` as an animated chip. No polling — WorkManager constraint `NetworkType.CONNECTED` handles timing.

*Decision:* WorkManager over a manual `ConnectivityManager` listener — WorkManager survives process death and device restart without any additional boot receiver wiring.

---

### 5. Reminder End-to-End Pipeline

```
"Remind me to call mom at 6 PM"
    │
    ▼  NLP parser (regex — no ML dependency)
CreateReminderUseCase
    │
    ├─→ ReminderRepository.insertReminder()  →  Room
    │
    └─→ ReminderScheduler.schedule()
            │
            ▼  AlarmManager.setExactAndAllowWhileIdle()
        ReminderAlarmReceiver.onReceive()
            │
            ▼  NotificationManagerCompat.notify()
        User notification at exact scheduled time
```

On Android 12+ the code checks `canScheduleExactAlarms()` and gracefully falls back to `set()` if the permission is not granted (SCHEDULE_EXACT_ALARM is a user-granted permission in Android 12+).

*Decision:* `AlarmManager` over `WorkManager` for reminders because WorkManager has a ~15-minute minimum interval floor and cannot guarantee exact delivery times, which matters for a reminder app.

---

### 6. Paging 3 with Reversed Layout

Chat messages are loaded 20 at a time via `PagingSource<Int, ChatMessageEntity>`. The `LazyColumn` uses `reverseLayout = true` so the latest message is always at the bottom without manually scrolling. `cachedIn(viewModelScope)` ensures the paging data survives configuration changes.

*Decision:* Paging 3 over manual offset queries — it handles load states (`Loading / NotLoading / Error`), retry, and prepend/append automatically. The `itemKey { it.id }` lambda gives Compose stable keys for `animateItem()` transitions.

---

### 7. HorizontalPager Onboarding with Validation-Gated Swipe

The onboarding uses `HorizontalPager` for native gesture swipe. Forward swipes are intercepted: if `ViewModel.goToNextStep()` returns `false` (validation failed), `pagerState.animateScrollToPage(currentStep)` bounces the pager back. The ViewModel holds all state so no data is lost on back-swipe or process recreation.

*Decision:* `HorizontalPager` over `AnimatedContent` because the assignment required gesture-based navigation, not just button-driven slide transitions. The pager gives the user physical ownership of the flow while validation still gates progression.

---

### 8. Voice Input — Dual-Channel Audio

When the mic is active, two systems run concurrently:

- `SpeechRecognizerManager` — wraps Android's `SpeechRecognizer` for speech-to-text
- `AudioRecorderManager` — runs `AudioRecord` at 16kHz PCM, computes RMS amplitude every 60ms, emits a normalised float via `callbackFlow`

They are independent. `SpeechRecognizer` cannot expose amplitude — it's a black-box API. `AudioRecord` provides the raw PCM data that drives the AuraCircle animation. Both are cancelled cleanly in `onCleared()`.

Speech results populate the **text field** rather than auto-sending, so the user can review and edit before committing.

---

## Tech Stack

```
Language          Kotlin 1.9+
UI                Jetpack Compose (Material 3)
Architecture      MVVM + Clean Architecture (Use Cases)
DI                Hilt
Database          Room (TypeConverters for custom types)
Preferences       DataStore Preferences
Async             Coroutines + Flow + StateFlow
Paging            Paging 3
Background work   WorkManager (Hilt Worker)
Audio             AudioRecord (PCM 16-bit), SpeechRecognizer
Alarms            AlarmManager (exact alarms)
Notifications     NotificationCompat, NotificationChannel
Navigation        Navigation Compose
Animation         Compose Animation APIs, Canvas DrawScope
```

No third-party networking, no Firebase, no Lottie, no ML Kit. Intentionally minimal dependencies — the focus is on demonstrating platform API mastery.

---

## Project Structure Highlights

```kotlin
// Coroutine state machine — every message through this pipeline
Typing → Validating → Processing → Responding → Idle
                                       ↑ 8s timeout → Error

// Sealed state with retry payload
data class Error(val message: String, val retryInput: String?) : AssistantState()

// Type-safe Room TypeConverter
@TypeConverter fun fromMessageMeta(meta: MessageMeta): String = gson.toJson(meta)

// Amplitude → canvas in one reactive chain
AudioRecord PCM → RMS normalise → callbackFlow → StateFlow
  → animateFloatAsState(spring) → Canvas drawCircle radius/alpha

// WorkManager with Hilt injection
@HiltWorker class SyncWorker @AssistedInject constructor(...)
```

---

## Setup

```bash
git clone <repo>
# Open in Android Studio Hedgehog or later
# minSdk 26, targetSdk 34
# No API keys required — fully offline
```

Requires: `RECORD_AUDIO`, `POST_NOTIFICATIONS` (Android 13+), `SCHEDULE_EXACT_ALARM` (Android 12+), `RECEIVE_BOOT_COMPLETED`.

---

*Built by a developer who believes architecture is a communication tool — it should be as readable to the next engineer as it is correct to the compiler.*
