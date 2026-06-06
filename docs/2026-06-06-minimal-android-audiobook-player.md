---
id: 2026-06-06-minimal-android-audiobook-player
title: Minimal Android audiobook player for local M4B and MP3 files
created: 2026-06-06
updated: 2026-06-06
tags:
  - android
  - audiobook
  - audio
  - player
  - m4b
  - mp3
status: captured
---

# Minimal Android audiobook player for local M4B and MP3 files

## Summary

A personal-project Android audiobook player with a restrained, pleasant interface and only the core controls needed for local listening: library view, book detail view, play or pause, skip forward or back 30 seconds, previous or next chapter, playback speed, and persistent progress. It should prioritize calm design and reliable playback over a large feature surface.

## Raw input

"/new a minimalistic audiobook player app with pleasant design as a personal project. Can read m4b and mp3 files, and has basic functionality: plus minus 30 sec, previous or next chapter, playback speed, library view, single book view. Not much else.
/enrich at the same time by highlighting possible difficulties with implementation."

## Clean idea statement

Build a simple Android audiobook player for locally stored `m4b` and `mp3` audiobooks, with a small but polished feature set focused on browsing books, controlling playback, navigating chapters, and resuming where the listener left off.

## Problem or opportunity

Many audio players are either too generic for audiobooks or overloaded with features. A focused personal app could offer a cleaner experience for local audiobook files while also serving as a manageable Android media project with clear technical boundaries.

## Notes

- Product shape:
  An offline-first audiobook app for personal libraries, optimized for long-form listening rather than music playback. The design goal should be quiet and readable, with clear typography, strong cover art presentation, and only a few obvious controls.
- Core screens:
  1. Library view showing cover, title, author if available, and progress.
  2. Single book view with larger cover, chapter list, progress bar, playback controls, and speed selector.
- Core controls:
  play or pause, seek plus or minus 30 seconds, previous or next chapter, playback speed, scrub within current file or chapter, and resume from saved position.
- File support goal:
  single-file `m4b` audiobooks with embedded chapters where possible, plus `mp3` audiobooks as either single files or folders of tracks.
- MVP feature plan:
  1. Import local files through Android file picker.
  2. Parse basic metadata such as title, album, author, duration, and cover art when available.
  3. Build a library of imported audiobooks.
  4. Support playback in background with notification and lock-screen controls.
  5. Save current position per book.
  6. Expose chapter navigation when chapters exist.
  7. Allow speed adjustment and 30-second skip controls.
- Non-goals for the first version:
  no cloud sync, no store integration, no social features, no recommendations, no DRM support, no advanced bookmarking system unless it falls out naturally.
- Design direction:
  Minimal, warm, and editorial. Avoid cluttered equalizer-style UI. Use strong spacing, clear hierarchy, and tactile transport controls that feel closer to a reading app than a music player.

## Research notes

- Android's current recommended playback stack is Jetpack Media3 with ExoPlayer. The Android Developers documentation explicitly recommends using ExoPlayer from Media3 for playback apps and provides background playback support through `MediaSessionService`. Sources: Android Developers basic playback guide and background playback guide. https://developer.android.com/media/implement/playback-app https://developer.android.com/media/media3/session/background-playback
- Media3's supported-format documentation lists `MP4`, `M4A`, and `MP3` as supported progressive container formats. Since `m4b` is effectively an audiobook-oriented MPEG-4 audio container variant, this is a good feasibility signal for raw playback, but the documentation does not explicitly call out `m4b` audiobook semantics. Source: Android Developers supported formats. https://developer.android.com/media/media3/exoplayer/supported-formats
- The same Media3 page notes that some formats such as `MP3` may only be seekable via constant-bitrate seeking in some cases, and that this behavior is not enabled by default. That matters because fast, reliable scrubbing is especially important for audiobooks. Source: Android Developers supported formats. https://developer.android.com/media/media3/exoplayer/supported-formats
- Android storage access is also a real implementation concern. Android recommends the Storage Access Framework for documents and other files, and documents that apps can interact with external storage and cloud-based storage through this framework. That is relevant if users should be able to import audiobook files from arbitrary folders or providers. Sources: Android Developers on Storage Access Framework and shared storage. https://developer.android.com/guide/topics/providers/document-provider https://developer.android.com/training/data-storage/shared/documents-files
- I found strong market evidence that dedicated audiobook apps do support `m4b` and chapter-aware playback, but these sources are product claims, not Android platform guarantees. Examples include LudyRead, Grimmory, and Homer Audio Player. This suggests the feature is achievable, but not necessarily trivial. Sources: official product pages. https://ludy.app/ludyread https://grimmory.org/docs/readers/audiobook-player https://homeraudioplayer.app/audiobooks
- Inference:
  the likely hard part is not ordinary audio playback, but reliable chapter extraction, metadata normalization, and treating different file layouts as one logical audiobook.

## Criticism and risks

- `m4b` playback itself is probably tractable, but `m4b` chapter handling may be the real difficulty. The Android Media3 supported-format docs confirm container support at a high level, but they do not clearly promise audiobook chapter parsing or chapter-title extraction. You may need custom metadata parsing or fallback behavior when chapter data is missing or inconsistent.
- `mp3` support sounds simple, but audiobook libraries often come as many separate `mp3` files per book. Grouping those tracks into one logical book, ordering them correctly, and maintaining a single resume position can get messy quickly.
- Background playback is not optional for a real audiobook app. On Android this means using `MediaSessionService` and foreground-service behavior correctly, which adds non-trivial lifecycle and notification complexity even for a small app.
- Importing files from arbitrary folders is more annoying than it first appears because Android storage access is permission- and picker-driven. If the app should remember folders, import from SD cards, or handle cloud-backed document providers, that increases edge cases significantly.
- Seeking quality matters more for audiobooks than for short audio clips. The Media3 docs explicitly note seek limitations for some `mp3` cases, so skip and scrub behavior may feel inconsistent across files unless you constrain supported encodings or add careful fallback handling.
- Metadata quality in local audiobook files is often poor. Some books will have nice cover art and chapter names; others will not. The app needs sensible defaults so the library view does not feel broken when tags are sparse.
- A truly minimal app can still fail if progress persistence is wrong. For audiobooks, losing place even once damages trust more than many missing features would.

## Next steps

- Build a technical spike that answers four questions before doing full UI work:
  1. Can Media3 play your target `m4b` samples reliably?
  2. Can you extract chapter titles and timestamps from those files?
  3. How should multi-file `mp3` books be grouped?
  4. Does resume state remain correct across app restarts and background playback?
- Gather a small test corpus:
  1. one well-tagged single-file `m4b`
  2. one poorly tagged `m4b`
  3. one single-file `mp3`
  4. one folder-based multi-track `mp3` audiobook
- Decide whether V1 only supports manually imported books or also folder-based library scanning.
- Define a fallback chapter strategy:
  if no embedded chapters exist, treat each `mp3` file as a chapter or generate coarse chapter markers only from track boundaries.
- Keep the MVP narrow:
  library, book detail, playback service, chapter list, speed control, and rock-solid resume state.
- Useful later additions if the core works:
  sleep timer, bookmarks, Android Auto support, richer metadata editing, and optional OPDS or Audiobookshelf integration.

## Sources

- Android Developers: Create a basic media player app using Media3 ExoPlayer - https://developer.android.com/media/implement/playback-app
- Android Developers: Background playback with a MediaSessionService - https://developer.android.com/media/media3/session/background-playback
- Android Developers: Media3 supported formats - https://developer.android.com/media/media3/exoplayer/supported-formats
- Android Developers: Open files using the Storage Access Framework - https://developer.android.com/guide/topics/providers/document-provider
- Android Developers: Access documents and other files from shared storage - https://developer.android.com/training/data-storage/shared/documents-files
- LudyRead official site - https://ludy.app/ludyread
- Grimmory audiobook player docs - https://grimmory.org/docs/readers/audiobook-player
- Homer Audio Player audiobooks page - https://homeraudioplayer.app/audiobooks
