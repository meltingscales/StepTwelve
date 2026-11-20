# StepTwelve

*A private, offline outreach helper for 12-step fellowships.*

StepTwelve makes 12th-step outreach easy. It pulls the program people from your contacts, shows who you've called and when, and lets you jot a note after each call. No paper log, no spreadsheet, no cloud.

## Download

**[Get the latest APK from the Releases page](../../releases/latest)** (`steptwelve-<version>.apk`).

StepTwelve isn't on the Play Store (see *Non-goals*), so you install the APK directly. This is called sideloading:

1. On your Android phone, open the [Releases page](../../releases/latest) and tap `steptwelve-<version>.apk` to download it.
2. Open the downloaded file (from the browser's downloads or the Files app).
3. Android will say installs from this source are blocked. Tap **Settings**, turn on **Allow from this source** for your browser or Files app, then go back and tap **Install**.
4. Optional: turn that setting back off afterward. StepTwelve keeps working.

To update, install the newer APK the same way. Your notes and settings are kept, since every release is signed with the same key. Want update alerts without checking by hand? [Obtainium](https://github.com/ImranR98/Obtainium) can watch this repo's Releases page for you.

Each release also has a `.sha256` file if you want to verify the download (`sha256sum -c steptwelve-<version>.apk.sha256`).

> **Privacy first, always.** Contact names and call notes can reveal fellowship membership and very personal recovery details. StepTwelve:
> - has **no internet permission** and needs no network access of any kind
> - shows **no ads** and has **no analytics, crash reporting, or tracking SDKs**
> - keeps all its data in **app-private local storage**, protected by **Android's built-in device encryption**
> - never lets data leave the phone unless **you** export it yourself
>
> When a feature request conflicts with privacy, privacy wins.

---

## Features

### 1. Tag-filtered contact list
- Reads the device contacts and shows only contacts whose **display name** (and optionally the contact's note or organization field) matches one of your tags.
- **Default tags:** `12step`, `saa`, `aa`, `newcomer`, `sponsee`, `sponsor`.
- Tags are editable: add, remove, rename, reset to defaults.
- **Matching rules:**
  - Case-insensitive.
  - Whole-token matching, so `aa` matches "John AA" or "John (AA)" but **not** "Aaron". Tokens split on whitespace and punctuation (any character that is not a letter or digit, e.g. `()[]-_/.,|`).
  - Spacing and dashes are normalized, so `12step` also matches "12 step" and "12-step".
- Filter chips at the top of the list let you narrow to one or more tags.
- Search box for name filtering within the tagged set.

### 2. Call status at a glance
- Reads the system call log and matches calls to tagged contacts by phone number. Normalize with `PhoneNumberUtils.compare` or libphonenumber so that formats like `+1 (555) 123-4567` and `5551234567` match.
- Each contact row shows:
  - Last call date, relative ("today", "3 days ago").
  - Direction and outcome icon: outgoing/incoming/missed, connected vs. no answer (duration 0 means not connected).
  - A **"Called today"** check mark, so you can see where you are in the day's calls.
- **Sort options:** longest since last contact (default, so neglected people rise to the top), name, most recent.

### 3. Per-call notes
- After a call, attach a note to **that specific call log entry**, keyed by call log ID plus timestamp and number as a fallback, since IDs can change after a call-log restore.
- Optional quick tags on a note: `answered`, `voicemail`, `no answer`, `texted instead`, `follow up`.
- **Post-call prompt (optional, on by default):** when the app comes back to the foreground after you dialed a tagged contact from inside StepTwelve, show a "How did the call go?" sheet pre-filled with that call.

### 4. Call history per contact
- Contact detail screen with the full call history for that person (from the call log), newest first, with any notes attached inline.
- Quick actions: **Call** (`ACTION_DIAL`, so no `CALL_PHONE` permission), **Text** (`ACTION_SENDTO sms:`), **Add manual entry**.

### 5. Timelog
- A single chronological feed of all outreach across contacts: calls plus notes, grouped by day.
- Filter by tag, by contact, by outcome, or by date range.
- Good for reviewing your week before a meeting or with a sponsor.

### 6. Manual entries
- The system call log only knows about regular phone calls. WhatsApp, Signal, in-person chats after a meeting, and texts are invisible to it.
- "Add manual entry" logs an outreach event with a date/time, a type (`call (other app)`, `text`, `in person`), and a note. Manual entries appear in the contact history and the timelog just like real calls.

### 7. Daily outreach goal
- Set a daily goal (default **3 calls**). A home-screen header shows `2 / 3 today`.
- Counts outgoing calls to tagged contacts (connected or not, since reaching out is what counts) plus manual entries.
- Weekly summary card: calls made, people reached, notes written.
- Keep it gentle. No guilt messaging and no punishing streak resets. A missed day just shows as a missed day.

### 8. Reminders
- Optional daily reminder notification at a time you choose ("Time for outreach calls?").
- Optional per-contact follow-up: from a note, "Remind me to call again in N days."
- The notification text is generic by default ("StepTwelve reminder") so it doesn't reveal anything on the lock screen.

### 9. Privacy and discretion
- **No `INTERNET` permission** in the manifest. Treat this as a hard rule and fail CI if it's ever added.
- **Local-only storage** for everything the app creates (notes, manual entries, tags, settings), kept in app-private internal storage and protected by Android's default encryption. See *Security* below.
- **No third-party SDKs that phone home.** Only AndroidX/Jetpack and Kotlin libraries. Every dependency must work fully offline.
- **App lock:** optional biometric or device-credential unlock (`BiometricPrompt`) on open and after N minutes in the background.
- **Screen privacy:** optional `FLAG_SECURE`, which blocks screenshots and hides content in the recent-apps switcher.
- **Discreet launcher name and icon:** optional activity-alias that shows the app as **"Circle Back"** with a neutral icon in the launcher. In-app branding stays "StepTwelve".
- `android:allowBackup="false"` and data-extraction rules that exclude everything, so no silent cloud backup of notes.

### 10. Export / import as a ZIP file (user-controlled)
- **Export all app data to a single `.zip`** (for example `steptwelve-export-2026-11-20.zip`), saved wherever you choose through the Storage Access Framework (`ACTION_CREATE_DOCUMENT`). The app never picks a location or uploads anything itself.
- ZIP contents:
  ```
  manifest.json   # app version, export schema version, export timestamp
  notes.json      # all call notes and manual entries (full fidelity, used for import)
  notes.csv       # same data, human-readable, opens in any spreadsheet app
  tags.json       # tag list
  settings.json   # goal, reminder time, privacy toggles
  ```
- Contacts and the call log themselves are **not** exported, only the app's own data plus the minimum needed to re-link notes (normalized number, timestamp, contact display name at time of note).
- **Import** from the same ZIP format (via `ACTION_OPEN_DOCUMENT`) to restore or move to a new phone. Validate the schema version, show a preview ("42 notes, 4 tags"), and offer *merge* or *replace*.
- Before exporting, show a plain warning: *"This file contains your outreach notes. Once it leaves the app it is not protected by StepTwelve. Store it somewhere safe."*
- Optional (nice-to-have): password-protected ZIP using AES-256 (for example via zip4j, which is offline-only). Off by default.
- Build the ZIP with `java.util.zip` (no extra dependency needed for the default path).

---

## Non-goals
- No accounts, sync, cloud, or sharing features.
- No reading SMS content.
- No modifying the user's contacts or call log (read-only).
- No Play Store distribution for v1. Google Play restricts `READ_CALL_LOG` to default dialer and similar apps, so v1 targets **sideloading** from GitHub Releases (and possibly F-Droid later).

---

## Tech stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3, with dark theme and dynamic color
- **Architecture:** single-activity, MVVM, `ViewModel` + `StateFlow`, Navigation-Compose
- **DI:** Hilt (or manual DI if it keeps the project smaller)
- **Persistence:** Room (SQLite) for notes and tags, DataStore for settings, both in app-private internal storage
- **Export:** `java.util.zip` + kotlinx.serialization (JSON) and a small CSV writer
- **Background:** WorkManager for reminders
- **Min SDK:** 26, **Target SDK:** latest stable
- **Build:** Gradle Kotlin DSL with a version catalog (`libs.versions.toml`)

## Security and storage
- **Local storage only.** The Room database and DataStore files live in the app's private internal storage (`/data/data/<package>/`), which other apps cannot read.
- **Default Android encryption.** Rely on Android's built-in file-based encryption (mandatory on devices launched with Android 10+). Store everything in **credential-encrypted storage** (the default), so the data is encrypted at rest and unreadable until the phone is unlocked after boot. Do **not** use device-protected storage.
- No custom crypto layer for v1. That keeps the app simple and avoids hand-rolled crypto mistakes. App lock and `FLAG_SECURE` cover the "someone picks up my unlocked phone" case.
- **No leaks through backups:** `android:allowBackup="false"`, plus `dataExtractionRules` and `fullBackupContent` that exclude everything, so nothing goes to Google Drive backups or device-to-device transfer. The ZIP export is the only way data leaves the app.
- **No network at all:** no `INTERNET` or `ACCESS_NETWORK_STATE` permission. Add a CI/Gradle check that fails the build if either appears in the merged manifest (including permissions pulled in by libraries).
- Contacts and call log data is **read live from the system providers and never copied** into the database, except the minimum needed to anchor a note (call ID, timestamp, normalized number, contact lookup key).
- Never write note contents to Logcat. Strip logs in release builds.
- Write a short threat-model note in `SECURITY.md`: protects against other apps, cloud backups, and a locked or powered-off phone; does not protect against a rooted or compromised device, or against an exported ZIP once it leaves the app.

## Permissions
| Permission | Why | Required |
|---|---|---|
| `READ_CONTACTS` | Find tagged contacts | Yes |
| `READ_CALL_LOG` | Show call history and called-today status | Yes |
| `POST_NOTIFICATIONS` | Reminders (Android 13+) | Optional |
| `USE_BIOMETRIC` | App lock | Optional |

- Request permissions in context, with a short explanation screen first. The app must stay usable in a degraded mode if call log access is denied: contacts and manual entries still work.
- **No** `INTERNET`, `CALL_PHONE`, `READ_SMS`, or location permissions.

---

## Data model (Room)
```kotlin
@Entity data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,           // "saa"
    val enabled: Boolean = true
)

@Entity data class CallNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callLogId: Long?,         // CallLog.Calls._ID, null for manual entries
    val contactLookupKey: String, // ContactsContract lookup key (stable across syncs)
    val normalizedNumber: String,
    val timestamp: Long,          // call or entry time (epoch ms)
    val entryType: EntryType,     // PHONE_CALL, OTHER_APP_CALL, TEXT, IN_PERSON
    val outcome: Outcome?,        // ANSWERED, VOICEMAIL, NO_ANSWER, TEXTED, null
    val note: String,
    val followUpAt: Long?,        // optional reminder time
    val createdAt: Long,
    val updatedAt: Long
)
```
Settings (daily goal, reminder time, lock, FLAG_SECURE, discreet icon) go in Preferences DataStore in app-private storage.

## Screens
1. **Onboarding:** what the app does, privacy promise, permission requests, tag setup.
2. **Home / Contacts:** goal header, tag filter chips, search, sorted list with status.
3. **Contact detail:** call history with inline notes and quick actions.
4. **Note editor:** bottom sheet with outcome chips, note text, and an optional follow-up reminder.
5. **Timelog:** chronological feed with filters.
6. **Settings:** tags, goal, reminders, app lock, screen privacy, discreet launcher, export/import, about/licenses.

---

## Suggested milestones
1. **M1, skeleton:** project setup, Compose theme, navigation, permission flow, CI check that the manifest has no `INTERNET` permission.
2. **M2, contacts and tags:** tag CRUD, whole-token matcher with unit tests (the `aa` vs. "Aaron" cases), filtered contact list.
3. **M3, call log:** number normalization and matching, last-call status, called-today, sorting, contact detail history.
4. **M4, notes:** Room database in private storage, backup exclusion rules, note editor, notes inline in history, post-call prompt.
5. **M5, timelog and manual entries.**
6. **M6, goal, reminders, app lock, FLAG_SECURE, discreet alias.**
7. **M7, ZIP export/import,** then polish, accessibility pass (TalkBack, font scaling), and a signed `v1.0.0` release published on GitHub Releases.

**Status:** M1 done (skeleton, theme, navigation, contacts permission flow, CI with the no-network check, release pipeline). M2 done (tag CRUD backed by Room with default tags, whole-token matcher with unit tests, live tag-filtered contact list with chips and search). Matching against the contact's note/organization field is not built yet. Next up: M3.

## Testing
- Unit tests: tag matcher, phone number normalization and matching, goal counting, ZIP export → import round-trip (data identical after restore), schema-version handling.
- Instrumented tests: Room open and migrate, with a fake ContactsProvider and CallLog data.
- Manual QA: deny each permission and confirm graceful fallback; reboot and confirm reminders persist; confirm `adb backup` and cloud backup contain no app data; confirm the merged manifest has no network permissions; run with airplane mode on and confirm everything works.

## Building
Requires JDK 17 and the Android SDK (platform 36).
```bash
./gradlew assembleDebug        # debug APK at app/build/outputs/apk/debug/
./gradlew test                 # unit tests
./gradlew check                # unit tests, lint, and the no-network-permission manifest check
./gradlew assembleRelease      # signed release (needs keystore.properties, see below)
```
Install with `adb install app/build/outputs/apk/debug/app-debug.apk`, or copy the APK to the phone and allow installs from that source.

`assemble*` and `check` run `verify<Variant>NoNetworkPermissions`, which fails the build if the merged manifest (including library manifests) requests `INTERNET` or `ACCESS_NETWORK_STATE`.

## CI/CD and releases
- **CI** (`.github/workflows/ci.yml`): every push to `main` and every pull request runs `./gradlew check assembleDebug`.
- **Release** (`.github/workflows/release.yml`): pushing a tag like `v1.2.3` builds a signed release APK, verifies the signature, and publishes `steptwelve-1.2.3.apk` plus a `.sha256` checksum on the [Releases page](../../releases). A tag with a suffix (`v1.2.3-beta.1`) is published as a pre-release.
  - `versionName` comes from the tag (`1.2.3`), and `versionCode` is `major * 1000000 + minor * 1000 + patch`, so every release upgrades the previous one in place.

### One-time signing setup
Every release must be signed with the **same key**, or Android refuses to install the update over the old version (users would have to uninstall and lose their notes). Generate the key once and back it up somewhere safe:
```bash
keytool -genkeypair -v -keystore steptwelve-release.jks -alias steptwelve \
  -keyalg RSA -keysize 4096 -validity 10000
```
Add these repository secrets (Settings > Secrets and variables > Actions):

| Secret | Value |
|---|---|
| `STEPTWELVE_KEYSTORE_BASE64` | `base64 -w0 steptwelve-release.jks` |
| `STEPTWELVE_KEYSTORE_PASSWORD` | keystore password |
| `STEPTWELVE_KEY_ALIAS` | `steptwelve` |
| `STEPTWELVE_KEY_PASSWORD` | key password |

For local release builds, create `keystore.properties` in the repo root (git-ignored, never commit it):
```properties
storeFile=steptwelve-release.jks
storePassword=...
keyAlias=steptwelve
keyPassword=...
```

### Cutting a release
```bash
git tag v0.1.0
git push origin v0.1.0
```

---

## A note on spirit
This app exists to make picking up the "500-pound phone" a little lighter. It should feel calm and kind, never like a productivity tracker that scolds you. Reaching out counts, whether or not anyone answers.

## License
TBD (suggest GPL-3.0 or MIT).
