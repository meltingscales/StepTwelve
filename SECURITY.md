# Security and threat model

StepTwelve stores outreach notes that can reveal fellowship membership and personal recovery details. This note says what the app protects against and what it doesn't.

## Protects against
- **Other apps on the phone.** All app data (Room database, settings) lives in app-private internal storage, which other apps cannot read.
- **Network leaks.** The app has no `INTERNET` or `ACCESS_NETWORK_STATE` permission, and the build fails if any dependency adds one. No ads, analytics, crash reporting, or tracking SDKs.
- **Cloud and device-transfer backups.** `android:allowBackup="false"`, plus `dataExtractionRules` and `fullBackupContent` that exclude everything. Nothing goes to Google Drive backups or device-to-device transfer.
- **A locked or powered-off phone.** Data is stored in credential-encrypted storage (Android's default file-based encryption), so it is unreadable until the phone is unlocked after boot.
- **Someone picking up the unlocked phone:** optional app lock (fingerprint, face or device credential via `BiometricPrompt`, re-locks after a chosen time in the background) and optional `FLAG_SECURE` screen privacy.
- **The lock screen.** Reminder notifications use a generic public version ("StepTwelve reminder", or "Circle Back reminder" with the discreet icon), so contact names never show on a locked screen.
- **Spreadsheet formula injection** in the exported `notes.csv`: cells starting with `=`, `+`, `-` or `@` are prefixed with `'`.

## Does not protect against
- A rooted or otherwise compromised device.
- An exported ZIP once it leaves the app. The user chooses where it goes and is responsible for storing it safely.
- Someone who knows the phone's unlock credential (the app lock accepts it).
- Contact names themselves. Tags live in the system contacts app, which may sync them to a cloud account. StepTwelve only reads contacts and never changes them.

## Reporting a vulnerability
Please open a GitHub issue without sensitive details and ask for a private contact, or use GitHub's private vulnerability reporting if it's enabled for this repository.
