---
id: 2026-06-07-minimal-android-audiobook-player-status
title: Status snapshot for minimal Android audiobook player
created: 2026-06-07
updated: 2026-06-07
tags:
  - android
  - audiobook
  - audio
  - player
  - m4b
  - status
status: in-progress
---

# Status snapshot for minimal Android audiobook player

## Summary

The app is in strong MVP shape and is already usable for local `m4b` listening.

Core import, library, chapter navigation, playback, notification controls, persistence, and restart recovery are all implemented. The remaining work is mostly polish, broader QA, and optional feature expansion.

## Current status

### Implemented

1. Android project scaffold with Gradle, Compose, and app architecture baseline
2. Local `m4b` import via Storage Access Framework
3. Persisted library using Room
4. Metadata extraction for title, author, duration, and cover art
5. Duplicate import handling that updates an existing entry instead of creating a duplicate
6. Remove-from-library flow
7. `m4b` chapter extraction and persisted chapter storage
8. Book detail playback screen with chapter-relative scrubber
9. Background playback with Media3 and `MediaSessionService`
10. Play or pause
11. Seek backward and forward by 30 seconds
12. Playback speed control
13. Notification playback controls, including `-30s` and `+30s`
14. Progress persistence during playback
15. Restore active book, position, and speed after app restart or process death
16. Chapter picker dialog with tappable chapter rows
17. Compact single-screen playback layout with no scrolling

### Intentionally out of scope right now

1. `mp3` support
2. Sleep timer
3. Cloud sync
4. Bookmarks and notes
5. Folder scanning
6. Android Auto, Wear OS, or tablet-specific layouts

## UX status

The current UI is functional and fairly close to MVP expectations.

What is in good shape:

1. Library import flow works
2. Book playback screen fits on one screen
3. Chapter navigation is straightforward
4. Notification controls work
5. Resume behavior is solid enough for continued iteration

What is still rough:

1. Visual polish and typography hierarchy
2. Spacing balance across the playback screen
3. Library empty, loading, and edge-case messaging can still be improved
4. Some button styling and card styling can be made more intentional

## Technical status

What has been proven:

1. The app builds successfully with `.\gradlew.bat --no-daemon assembleDebug`
2. The APK installs and runs on the emulator
3. Local sample `m4b` import works
4. Playback works in foreground and background
5. Notification controls are functional
6. Progress and playback speed persist
7. Chapter-relative playback UI works in-app and in notification progress

Areas that still deserve broader validation:

1. More `m4b` files with different metadata and chapter layouts
2. Longer playback sessions
3. More interruption scenarios such as headset disconnects or incoming audio focus changes
4. Real device behavior in addition to emulator testing
5. Database migration strategy beyond destructive fallback

## Known limitations and caveats

1. The app currently targets `m4b` only
2. Chapter extraction depends on embedded chapter data being present and readable
3. The current Room setup still relies on destructive fallback migration during schema changes
4. Library management is intentionally minimal
5. The current UI is tuned for portrait phone use

## Suggested follow-ups

### Highest-value next steps

1. Do a focused UI polish pass on library and playback screens
2. Run a structured QA pass on multiple `m4b` files
3. Test on at least one real Android device
4. Replace destructive Room migration fallback with explicit migrations
5. Improve error handling around malformed files and failed metadata extraction

### Good optional improvements after MVP hardening

1. Better library sorting and filtering
2. Recently played or currently playing affordances in the library
3. More refined notification presentation
4. More resilient import/update messaging
5. Chapter list search or better chapter labeling for long books

### Nice-to-have future features

1. Sleep timer
2. Bookmarks
3. Notes or highlights
4. `mp3` support
5. Export or backup of library/progress

## Recommended next-session plan

If work resumes soon, a good order would be:

1. UI polish pass
2. QA and bug-fix pass
3. Real-device testing
4. Migration hardening
5. Decide whether to stop at MVP or add post-MVP features

## Pause point

This is a good pause point. The app is already functional enough to demonstrate the main product idea and continue iterating from a stable base.
