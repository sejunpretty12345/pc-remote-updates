# PhonePilot A17 Lite v0.2.0

This is a **permission-minimized, local-only prototype** for Samsung Galaxy A17. It is NOT a phone remote-control system and is NOT connected to this ChatGPT chat.

## Why this fork exists

Google Play Protect blocked the prior APK and warned that it could request access to sensitive information. The previous app declared an AccessibilityService, a foreground service and calendar-reading permissions. Rather than bypass Play Protect, this branch removes those capabilities entirely.

The launcher icon remains configured and the build checks for an APK signature and icon resources.

## Features

- Open YouTube searches using an Android VIEW intent
- Open Naver searches in the browser
- Launch allowlisted apps (YouTube, Samsung Notes, Chrome, Naver, Samsung Calendar) if available
- Share note text using Android's share sheet; **the user must choose an app and save the note**
- Optional home screen intent
- Explicit local approval dialog before actions
- Local history and emergency stop toggle

## Not supported

- ChatGPT chat-to-phone direct remote control
- Automatically writing or editing Samsung Notes
- Accessibility screen reading / touches / scrolling
- Google Calendar event command polling
- Verification on a physical Galaxy A17
- Google Play Protect approval: build success does not establish an app is safe or approved

No runtime permissions are requested in the AndroidManifest. In particular, no accessibility service is declared and no calendar, notification or foreground-service permission is requested.

## Build / install

The GitHub Actions workflow on branch `phonepilot-a17-safe` compiles a debug APK and verifies its signature/icon. Debug signing is for local development, not production distribution.

**If Play Protect blocks the APK, stop installation. Do not disable Play Protect or circumvent the warning.** Use a trusted channel and official review/distribution as appropriate.

A successful compile alone does not demonstrate device support or real-world functionality.
