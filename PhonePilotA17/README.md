# PhonePilot AI A17 — v0.3.0 assistant

Android app for Galaxy A17. **No Accessibility Service, root, ADB, device-administrator privilege, or QUERY_ALL_PACKAGES.**

## What actually works (Android-supported mechanisms)

- In-app Korean conversational command input with explicit execution confirmation
- Optional speech transcription through the device's speech recognizer, with a reviewable text field before sending
- Launch installed apps by visible launcher label (Android package-visibility `MAIN`/`LAUNCHER` query), including known app aliases
- YouTube search, Google/Naver browser search, map searches via intents
- Alarm/timer setup via system Clock intents (normal `SET_ALARM` permission), user confirms in Clock app
- Calendar event creation UI via `ACTION_INSERT`, user saves
- Wi-Fi/Bluetooth/display/battery settings panels (do not silently toggle)
- Media volume adjustment, camera launch, dialer (no call placed), SMS compose (no message sent)
- Local PhonePilot notes with direct save and retrieval; share-sheet to Samsung Notes (final saving done manually)
- Confirmation dialog for all executable commands, local history, emergency stop, 200-app picker
- Optional Gemini 3.5 Flash-Lite natural-language interpretation using user's *own* API key stored via Android Keystore AES-GCM; only unsupported command text sent to Google

## What Android will not let this app do without different privileged integration

- **Automatically edit existing Samsung Notes or touch arbitrary controls in other apps.** Sharing text is not direct editing.
- Read private contents of other apps; impersonate user; authorize purchases; send messages without confirmation.
- Directly connect this particular ChatGPT conversation to the phone. This build is **an in-app assistant** rather than a ChatGPT connector.
- Run a background virtual operator. The user initiates tasks in the foreground app.

### Security

- Normal permissions only: `INTERNET` (optional API) and `SET_ALARM` (Android Clock intent).
- No accessibility request, screen capture, notifications, calendar read/write, SMS sending, camera read, location read, contacts or phone-call permissions.
- Does not send installed-app lists or local notes to the Gemini API; sending any typed prompt requires user to submit it in chat.
- API key stored encrypted using device Android Keystore; not hardcoded into the APK.
- The remote model proposes one strictly validated command; confirmation is required before executing it.
- **Do not turn off Play Protect or bypass device restrictions if the APK installation is blocked.** Unreviewed sideloaded debug APKs may still be blocked, even without AccessibilityService.

### Build

GitHub Actions on `phonepilot-a17-assistant` uses Android Gradle Plugin 8.8.2, Gradle 8.10.2, Java 17 and Android SDK 35. Verifies debug APK signing, launcher icon and manifest forbidden permissions. Debug-signed APKs are not approved Play Store releases.

### Test plan

- [ ] Installed on physical SM-A175N (not possible remotely)
- [ ] Voice recognition and edit
- [ ] App label match (KakaoTalk/YouTube/Samsung Notes)
- [ ] Clock alarm and timer
- [ ] Calendar insert confirmation
- [ ] Samsung Notes sharing as separate user save action
- [ ] Gemini API key privacy, API response parsing, error handling
- [ ] Play Protect developer review / trusted distribution

A successful GitHub Actions build verifies compile, not on-device functionality or Play Protect approval.
