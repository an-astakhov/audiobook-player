---
id: 2026-06-06-minimal-android-audiobook-player-implementation-plan
title: Implementation plan for minimal Android audiobook player
created: 2026-06-06
updated: 2026-06-06
tags:
  - android
  - audiobook
  - audio
  - player
  - m4b
  - implementation-plan
status: planned
---

# Implementation plan for minimal Android audiobook player

## Scope

This document turns the rough product spec into a concrete implementation plan for a personal Android app focused on local `m4b` audiobooks only.

V1 supports:

1. Importing local `m4b` files through the Android file picker
2. Building a small on-device library from imported books
3. Displaying metadata, cover art, duration, progress, and chapters when available
4. Playing books in the foreground and background
5. Play or pause, seek back or forward 30 seconds, scrub, previous or next chapter, and playback speed control
6. Saving and restoring listening progress reliably

V1 explicitly does not support:

1. `mp3`
2. Folder scanning
3. DRM
4. Cloud sync
5. Bookmarks, notes, sleep timer, Android Auto, Wear OS, or recommendations

## Product goals

The app should feel calm, small, and trustworthy. For V1, reliability matters more than feature count.

Success criteria:

1. A user can import an `m4b`, close the app, reopen it, and continue from the same spot
2. Background playback and notification controls work consistently
3. Chapter navigation works for `m4b` files with embedded chapter data
4. The app stays usable even when metadata is incomplete
5. The UI remains visually restrained and easy to scan on phones

## Working assumptions

These assumptions keep the first build practical:

1. Platform target:
   Android phone first, portrait-first UI, no tablet-specific layouts in V1
2. Tech baseline:
   Kotlin, Jetpack Compose, Media3, Room, Coroutines and Flow
3. Storage model:
   User imports individual files using Storage Access Framework and the app persists URI access
4. Book model:
   Each imported `m4b` file is one logical book
5. Chapter model:
   Chapters come only from embedded `m4b` metadata; if chapter parsing fails, the book remains playable and chapter controls degrade gracefully
6. App structure:
   Single app module for V1 with clear package boundaries; avoid early multi-module overhead

## Key technical decisions

### 1. Playback stack

Use Jetpack Media3 with ExoPlayer and a `MediaSessionService`.

Why:

1. Native fit for modern Android playback
2. Solid support for background playback, notifications, audio focus, and media controls
3. Cleaner long-term path than building playback around lower-level platform APIs

### 2. UI stack

Use Jetpack Compose with Navigation Compose.

Why:

1. Faster iteration for a small personal project
2. Easier to build a minimal, editorial-looking interface
3. Straightforward state handling for library and player screens

### 3. Persistence

Use Room as the source of truth for imported books, chapters, and progress.

Why:

1. Strong fit for structured offline data
2. Easy to query library state reactively
3. Reliable migration path if fields evolve later

### 4. Dependency management

Keep DI lightweight for V1.

Recommendation:

1. Use constructor injection and a small app container
2. Avoid Hilt unless setup complexity starts paying for itself

## Architecture overview

Use a simple layered architecture inside a single module:

1. `ui`
   Compose screens, navigation, components, theming, and screen state models
2. `domain`
   Use cases such as import book, observe library, save progress, jump to chapter, and change playback speed
3. `data`
   Room entities and DAOs, repository implementations, metadata extraction, chapter parsing, cover extraction, and import coordination
4. `playback`
   Media3 player wrapper, `MediaSessionService`, notification integration, playback state bridge, and queue logic
5. `platform`
   SAF helpers, URI permission handling, and Android-specific adapters

### Recommended package layout

```text
app/src/main/java/.../
  MainActivity.kt
  AppContainer.kt
  ui/
    theme/
    library/
    book/
    components/
  domain/
    model/
    usecase/
  data/
    db/
    repository/
    importing/
    metadata/
    chapters/
  playback/
    service/
    controller/
    model/
  platform/
    storage/
    image/
```

## Core user flows

### 1. Import book

1. User taps `Import book`
2. App launches `ACTION_OPEN_DOCUMENT`
3. User selects an `m4b`
4. App persists URI permission
5. App extracts metadata, cover art, duration, and chapters
6. App saves the book to the database
7. Library updates immediately

### 2. Start playback

1. User opens a book from the library
2. App loads the book detail screen
3. User taps play
4. App starts playback from saved position if present
5. Notification and lock-screen controls appear

### 3. Resume later

1. App saves progress periodically and on lifecycle changes
2. On reopen, the current book and last position are restored
3. Library and book detail screens show updated progress

### 4. Chapter navigation

1. User sees a chapter list when chapter metadata exists
2. User can jump from the list or use previous or next chapter controls
3. If a book has no chapter data, chapter UI is hidden or disabled with no broken state

## Data model

### Book

Suggested fields:

1. `id`
2. `contentUri`
3. `displayName`
4. `title`
5. `author`
6. `album`
7. `durationMs`
8. `coverImagePath`
9. `fileSizeBytes`
10. `lastModified`
11. `dateImported`
12. `lastPlayedAt`
13. `currentPositionMs`
14. `completionPercent`
15. `playbackSpeed`
16. `chapterCount`
17. `hasChapters`

### Chapter

Suggested fields:

1. `id`
2. `bookId`
3. `chapterIndex`
4. `title`
5. `startMs`
6. `endMs`
7. `durationMs`

### Playback session state

Suggested persisted state:

1. `activeBookId`
2. `lastKnownPositionMs`
3. `lastKnownSpeed`
4. `isCompleted`

## Metadata and chapter extraction strategy

### Metadata extraction

Use Android-friendly metadata extraction for:

1. title
2. author or artist
3. album
4. duration
5. embedded artwork

Implementation notes:

1. Read file metadata from the selected content URI, not file paths
2. Cache extracted cover art into app-private storage and keep only a path or key in the database
3. Normalize missing metadata with sensible fallbacks such as filename-based title

### Chapter extraction

This is the main technical risk in the project.

Plan:

1. Run an early spike against real sample `m4b` files
2. Check whether chapter metadata is available directly through Media3 or Android metadata APIs
3. If not, implement or integrate an `m4b` chapter parser for MPEG-4 chapter atoms
4. Convert extracted chapter markers into a normalized local `Chapter` table

Fallback behavior:

1. Playback must still work if chapter parsing fails
2. Chapter list and previous or next chapter controls should disable cleanly for that book
3. Import should store a parsing warning for debugging, not crash the UI

## Playback design

### Service model

Use a foreground `MediaSessionService` for all actual playback.

Responsibilities:

1. Own the ExoPlayer instance
2. Expose media session controls to notification and lock screen
3. Manage audio focus and noisy-audio events
4. Publish playback state to the app UI
5. Persist progress at safe intervals

### Control surface

Player controls in V1:

1. play or pause
2. back 30 seconds
3. forward 30 seconds
4. previous chapter
5. next chapter
6. playback speed selector
7. seek bar scrubber

### Progress persistence rules

Update progress:

1. every 10 to 15 seconds while playing
2. when playback pauses
3. when the current item changes
4. when the app or service is stopping

Completion behavior:

1. Mark a book completed near the end, for example after 98 percent or within a short end threshold
2. Offer restart-from-beginning behavior when replaying a completed book

## UI plan

### Design direction

The app should feel closer to a reading app than a music player.

Principles:

1. Strong typography
2. Warm neutral colors
3. Spacious layout
4. Large, obvious transport controls
5. Cover art treated as a focal point, not decoration
6. Minimal chrome and low visual noise

### Screen 1: Library

Contents:

1. App title
2. Import button
3. Scrollable list of books
4. Each row shows cover, title, author, progress, and optional duration
5. Empty state for first launch

Behavior:

1. Tap a book to open its detail screen
2. Most recently played books appear first by default

### Screen 2: Book detail and player

Contents:

1. Large cover
2. Title and author
3. Current progress and total duration
4. Seek bar
5. Primary transport controls
6. Playback speed control
7. Chapter list when available

Behavior:

1. Opens at the current saved position
2. Chapter tap jumps immediately
3. Speed selection persists per book or globally, depending on final preference

Recommendation:

1. Keep playback speed global for V1 unless per-book speed proves important

## State management

Use `ViewModel` plus `StateFlow` per screen.

Recommended screen states:

1. `LibraryUiState`
   loading, empty, list of books, import-in-progress, import error
2. `BookUiState`
   metadata, chapter list, current playback state, progress, and availability of chapter controls

Playback state should be streamed from the service through a controller layer rather than letting UI talk to ExoPlayer directly.

## Error handling

Plan for boring failures explicitly.

Common cases:

1. URI permission lost
2. User imports unsupported or malformed file
3. Metadata missing
4. Cover art missing
5. Chapter parsing partial or empty
6. File becomes unavailable after import

User-facing behavior:

1. Show readable error messages
2. Keep the rest of the library functional
3. Never delete progress silently
4. Let a broken book be removed from the library cleanly

## Delivery phases

### Phase 0: Technical spike

Goal: prove the risky pieces before heavy UI work

Tasks:

1. Create a tiny playback prototype using one sample `m4b`
2. Validate `m4b` playback through Media3
3. Probe metadata extraction
4. Probe chapter extraction on at least two different sample files
5. Document actual supported chapter formats and failure modes

Exit criteria:

1. `m4b` playback works
2. Metadata extraction approach is chosen
3. Chapter strategy is chosen
4. Fallback behavior is defined if chapter parsing is inconsistent

### Phase 1: Project bootstrap

Tasks:

1. Create Android project structure
2. Add Compose, Media3, Room, and testing dependencies
3. Set up theme, navigation shell, and app container
4. Add basic logging and debug helpers

Exit criteria:

1. App launches into a working empty library screen

### Phase 2: Import and library

Tasks:

1. Implement file picker flow
2. Persist URI permissions
3. Build import pipeline
4. Extract and store metadata
5. Extract and cache cover art
6. Save books in Room
7. Render library list and empty state

Exit criteria:

1. User can import an `m4b` and see it in the library

### Phase 3: Playback foundation

Tasks:

1. Implement `MediaSessionService`
2. Connect UI to playback controller
3. Support play or pause, 30-second seek, and scrubbing
4. Show notification and lock-screen controls
5. Handle audio focus correctly

Exit criteria:

1. Playback continues reliably in background
2. Notification controls work

### Phase 4: Chapters and book detail

Tasks:

1. Build book detail screen
2. Display chapter list when available
3. Implement previous or next chapter actions
4. Highlight current chapter during playback
5. Handle no-chapter books cleanly

Exit criteria:

1. Chapter navigation works on supported `m4b` files

### Phase 5: Progress and polish

Tasks:

1. Persist playback position reliably
2. Restore active book and position
3. Add playback speed selector
4. Improve loading, import, and error states
5. Refine typography, spacing, and motion

Exit criteria:

1. Resume behavior is trustworthy across app restarts
2. UI feels cohesive and intentionally minimal

### Phase 6: Hardening

Tasks:

1. Run manual QA on a real-device test corpus
2. Fix lifecycle edge cases
3. Verify battery and background behavior
4. Add migrations if schema changed during development
5. Prepare release build and app icon

Exit criteria:

1. App is stable enough for personal daily use

## Testing plan

### Test corpus

Gather at least:

1. one well-tagged `m4b` with cover and chapters
2. one `m4b` with chapters but sparse metadata
3. one `m4b` with missing or broken chapter data
4. one long-duration `m4b` to validate seeking and resume over time

### Unit tests

Focus on:

1. metadata normalization
2. chapter mapping
3. progress calculations
4. library sorting
5. completion threshold logic

### Instrumented tests

Focus on:

1. import flow with persisted URI access
2. opening book detail after import
3. restoring progress after app restart
4. playback controls updating UI state

### Manual QA checklist

1. import same file twice
2. remove file after import
3. rotate screen during playback
4. lock phone during playback
5. unplug headphones or disconnect Bluetooth
6. seek near start and near end
7. switch speeds repeatedly
8. test chapter jumps back-to-back

## Risks and mitigation

### Risk 1: `m4b` chapter parsing is inconsistent

Mitigation:

1. Spike first
2. Keep playback independent from chapter extraction
3. Design UI to degrade cleanly when chapters are unavailable

### Risk 2: progress persistence is flaky

Mitigation:

1. Persist from the playback service, not only from UI
2. Save on interval plus lifecycle transitions
3. Test process death and relaunch explicitly

### Risk 3: SAF and URI permissions cause edge cases

Mitigation:

1. Persist URI permissions immediately after import
2. Detect revoked or broken URIs on access
3. Show a repair or remove path for broken library items

### Risk 4: metadata quality is poor

Mitigation:

1. Use fallback title from filename
2. Tolerate missing author and cover art
3. Keep the library UI readable without rich metadata

## Suggested timeline

For a personal project, a realistic first pass is roughly 2 to 4 weeks of part-time work.

Rough breakdown:

1. Phase 0: 1 to 3 days
2. Phase 1: 1 day
3. Phase 2: 2 to 4 days
4. Phase 3: 3 to 5 days
5. Phase 4: 3 to 5 days
6. Phase 5: 2 to 4 days
7. Phase 6: 2 to 3 days

The biggest schedule variable is chapter extraction.

## Definition of done for V1

V1 is done when all of the following are true:

1. A user can import one or more local `m4b` files
2. Library entries render reliably with sane metadata fallbacks
3. A book plays in the background with media controls
4. Progress survives app restarts
5. Chapter navigation works on supported files and fails gracefully on unsupported ones
6. The UI is polished enough to feel intentionally minimal, not unfinished
7. The app is stable on a small real-device test corpus

## Recommended next step

Start with Phase 0 before writing much app UI.

If the spike shows that chapter extraction is straightforward, continue with the plan as written.
If the spike shows that chapter extraction is messy, keep the same architecture but decide whether V1 should:

1. ship with best-effort chapters and graceful fallback, or
2. narrow further to playback plus resume first, then add chapters in V1.1
