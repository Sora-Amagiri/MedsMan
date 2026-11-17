# MedsMan

A calm, local-first medication manager for Android. MedsMan keeps daily doses, pill
inventory, refill alerts, and adherence history in one place, wrapped in a Material 3
interface with hand-drawn tranquil nature scenes.

> MedsMan is a personal tracking tool, not medical advice. Always follow your
> clinician's instructions.

## Features

### Today
- Daily schedule cards with **Taken**, **Missed**, and **Skipped** actions
- Animated adherence ring and low-stock summary on a nature-artwork header
- Tap the nature icon to switch between Misty Mountains, Tranquil Sunrise, and Serene Forest
- Profile sheet for personal, emergency-contact, and doctor information

### Inventory
- Adaptive pill grid (single column on phones, multi-column on larger screens)
- Live stock counts with one-tap increment/decrement
- Refill dialog with quick amounts
- Configurable reorder threshold, refill-reminder toggle, pharmacy name/phone
- Low-stock banner and notification when stock crosses the threshold
- Add, edit, and delete medications, including reminder times and starting stock

### History
- Adherence overview with animated percentage ring
- Status breakdown for taken, missed, skipped, and snoozed doses
- Chronological log grouped by day with status filters
- Delete individual records or clear the whole history

### App-wide
- Adaptive navigation: bottom bar on phones, navigation rail on medium windows,
  permanent drawer on expanded windows
- Navigation 3 state-driven back stack; system back returns to Today
- All data stored locally in Room
- Dosage reminders via `AlarmManager`, with Take / Skip / Snooze notification actions
- Empty by default — no sample or demo data is created

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation 3 |
| Adaptive layout | Compose Material Adaptive / `NavigationSuiteScaffold` |
| State | Coroutines, `StateFlow`, ViewModel |
| Persistence | Room (KSP) |
| Reminders | `AlarmManager` + `BroadcastReceiver` + notifications |
| Build | Gradle 9.6, AGP 9.5, KSP |

## Requirements

- Android Studio with support for AGP 9.5 (or newer)
- JDK 25 (Android Studio's bundled JDK works)
- Android SDK 37
- A device or emulator running **Android 16 (API 36) or newer**

## Getting started

```bash
git clone https://github.com/Sora-Amagiri/MedsMan.git
cd MedsMan
./gradlew assembleDebug
```

Then open the project in Android Studio and press **Run**, or install from the
command line:

```bash
./gradlew installDebug
```

The app starts empty by design. Open **Inventory**, tap **+**, and add your first
medication with its dosage, reminder times, stock, and refill threshold.

`local.properties` is created by Android Studio and is intentionally not committed.

## Permissions

- **POST_NOTIFICATIONS** — dosage and refill reminders
- **SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM** — on-time dose reminders; the app falls
  back to inexact alarms when exact alarms are not allowed
- **RECEIVE_BOOT_COMPLETED** — re-schedules reminders after a reboot
- **VIBRATE / WAKE_LOCK / INTERNET** — declared by the template; reminders use
  vibration and wake locks

## Project structure

```text
app/src/main/java/com/sora/medsman/
├── data/
│   ├── local/          Room database, DAOs, entities, type converters
│   ├── model/          Domain models
│   └── repository/     Repository interfaces and implementation
├── notification/       Alarm scheduling, dosage receiver, boot receiver
├── ui/
│   ├── components/     Nature artwork, schedule cards, editor sheet, profile sheet
│   ├── navigation/     Navigation 3 destinations
│   ├── screens/        Today, Inventory, History
│   ├── theme/          Color, type, Material 3 theme
│   └── viewmodel/      StateFlow-driven ViewModels
├── MainActivity.kt
└── MedsManApplication.kt
```

## Data & privacy

MedsMan has no account, backend, sync, or analytics. Medication, inventory, and
history data live in a local Room database (`medsman_database`). The database starts
empty; a one-time migration removes the demo records that shipped in v1 while
preserving any user-created data.

## Verification

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Unit tests cover the Room type converters and core models. Lint passes with zero
errors.

## Known limitations

- Requires Android 16 (API 36) or newer.
- Notifications require the runtime notification permission; without it, reminders
  are scheduled but silent.
- Cloud backup, export, and multi-device sync are not implemented.
