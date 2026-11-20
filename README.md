# StepTwelve

*A private, offline outreach helper for 12-step fellowships.*

StepTwelve helps you pick up the phone. It finds the program people in your contacts, shows how long it's been since you were in touch with each of them, and lets you jot a quick note after a call. No paper log, no spreadsheet, no cloud.

## Download

**[Get the latest APK from the Releases page](../../releases/latest)** (`steptwelve-<version>.apk`).

StepTwelve isn't on the Play Store, because Google only allows call-log access for phone and dialer apps. You install the APK directly instead, which is called sideloading:

1. On your Android phone, open the [Releases page](../../releases/latest) and tap `steptwelve-<version>.apk` to download it.
2. Open the downloaded file from your browser's downloads or the Files app.
3. Android will say installs from this source are blocked. Tap **Settings**, turn on **Allow from this source** for your browser or Files app, then go back and tap **Install**.
4. Optional: turn that setting back off afterward. StepTwelve keeps working.

To update, install the newer APK the same way. Your notes and settings are kept. Want update alerts without checking by hand? [Obtainium](https://github.com/ImranR98/Obtainium) can watch this repo's Releases page for you.

Each release also has a `.sha256` file if you want to verify the download (`sha256sum -c steptwelve-<version>.apk.sha256`).

Requires Android 8.0 or newer.

## Privacy

Contact names and call notes can reveal fellowship membership and very personal recovery details. So StepTwelve:

- has **no internet permission** at all, so it can't send anything anywhere
- has **no ads, analytics, crash reporting or tracking**
- keeps your notes in **app-private storage** on your phone, encrypted by Android, and **never backs them up to the cloud**
- only **reads** your contacts and call log, and never changes them
- never lets your data leave the phone unless **you** export it

Optional extras in Settings: an **app lock** (fingerprint, face or screen lock), **screen privacy** (blocks screenshots and hides the app in recent apps), and a **discreet icon** that shows the app as "Circle Back" on your home screen. Reminder notifications only say "reminder", so nothing shows on your lock screen.

## How it works

**Tag your contacts by name.** StepTwelve shows contacts whose names include one of your tags as a whole word, like "Sam AA", "Jo (SAA)" or "Chris sponsor". "AA" matches "John AA" but not "Aaron". The default tags are `12step`, `saa`, `aa`, `newcomer`, `sponsee` and `sponsor`, and you can add, rename or turn off tags any time.

**See who you haven't talked to in a while.** The contact list puts the people you've gone longest without contacting at the top, with a small "3d ago" or "never" next to each name. A check mark shows who you've reached out to today. You can also sort by name or most recent.

**Call or text from the app.** Tap a contact to see your history with them, then tap **Call** or **Text**. After a call you started from StepTwelve, it asks how the call went so you can add a note while it's fresh.

**Log the calls the phone doesn't know about.** WhatsApp or Signal calls, texts, and chats after a meeting don't show up in your call log. Add them as a manual entry so they count.

**Set a gentle daily goal.** The header shows something like `2 / 3 today` and a summary of your week. Every reach-out counts, whether or not anyone answers. There are no streaks to lose and no guilt messages.

**Look back at your week.** The Timelog lists all your outreach by day, with filters for date range, outcome, tag and person. It's handy before a meeting or a check-in with your sponsor.

**Reminders.** Optionally get a daily nudge at a time you choose, or ask to be reminded to call someone again in a few days.

**Back up and move phones.** Settings > Export saves a ZIP of your notes, entries, tags and settings wherever you choose. Import it on your new phone. The ZIP is not protected once it leaves the app, so keep it somewhere safe.

## Permissions

| Permission | Why |
|---|---|
| Contacts | Find your tagged contacts (read-only) |
| Call log | Show when you last called each person (read-only, optional) |
| Notifications | Reminders (optional) |

If you don't allow call log access, contacts, notes and manual entries still work.

## FAQ

**Are my notes backed up to Google?** No. Cloud backup and device-to-device transfer are turned off for StepTwelve on purpose. Use Export to keep your own backup.

**Can StepTwelve see my texts?** No. It never reads messages.

**Does it work offline?** It only works offline. There's no internet permission to begin with.

## For developers

Build instructions, architecture, the full spec, the release pipeline and the security model are in [README.technical.md](README.technical.md) and [SECURITY.md](SECURITY.md).

## License

Released into the public domain under the [Unlicense](LICENSE).
