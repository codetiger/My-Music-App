# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A standalone Android music player for one listener aged 60+ (Xiaomi / Redmi / Poco on HyperOS is the main target). Songs are added from YouTube / Facebook / Instagram / MP3 links, WhatsApp shares or local files, downloaded on the phone with yt-dlp, and played offline. No server, accounts, analytics, logs or API keys. Distributed as an APK via GitHub Releases (Play Store not allowed), GPL-3.0.

Source of truth for behaviour: `highlevel-requirements.md`. Requirement IDs (ADD-11, PLY-9, UPD-6, LIB-3…) are cited in KDoc throughout the code; keep citing them when adding or changing behaviour. Visual rules: `design-system/README.md` (and `tokens.json`, `components/<Name>/README.md`); its "Changes to the requirements" section overrides the requirements doc where they conflict (e.g. 18sp body text, one light theme, four colours).

## Commands

Requires JDK 17 and Android SDK Platform 37.2 / Build-Tools 37. minSdk 35, targetSdk 37, arm64-v8a only.

```sh
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # install on connected phone/emulator
./gradlew testDebugUnitTest      # all unit tests (JVM only; no instrumented tests)
./gradlew testDebugUnitTest --tests 'com.codetiger.mymusicapp.add.LinkParserTest'   # one class
./gradlew testDebugUnitTest --tests '*LinkParserTest.someTestName'                  # one test
./gradlew lintDebug
./gradlew assembleRelease        # needs keystore.properties in project root (not in git)
```

Debug logs go to logcat tag `MyMusic` via `util/DebugLog` only; release builds must log nothing. Don't use `android.util.Log` directly.

Unit tests run on the JVM: Android's `org.json` is a stub there, so the real `org.json` is a test dependency. Keep testable logic (link parsing, ordering, error classification, XDoG, text size) in plain Kotlin free of Android framework calls.

Room schemas are exported to `app/schemas/`; a schema change needs a version bump and migration so libraries survive app updates (UPD-8).

Release: bump `versionCode`/`versionName` in `app/build.gradle.kts`, build signed release, create GitHub release tagged `v<versionName>` with the APK attached. The in-app updater compares that tag and `versionCode`; the repo is set by `BuildConfig.GITHUB_REPO`.

## Architecture

Single module `:app`, package `com.codetiger.mymusicapp`. No DI framework: `AppContainer` constructs every service once; `MyMusicApplication.container` holds it. Composables get it through `LocalApp` (plus `LocalNav`, `LocalUi` in `ui/AppUi.kt`); workers and the playback service fetch it from the Application.

- **`data/`** — Room DB (`db/Entities.kt`, `Daos.kt`), `LibraryRepository` (lists, favourites, Recently Removed with 30-day purge), DataStore-backed `SettingsRepository` and `PlayerStateStore`, `AppFiles` (private audio/part-file paths), `RestoreCheck` (after Auto Backup restore: re-queue link songs, drop WhatsApp/file songs and record them for the Home card). `ListRef` identifies a playable list (`Stored` playlist, or the derived `AllSongs` / `RecentlyPlayed`) and is encoded into nav routes.
- **`add/`** — `LinkParser` finds supported links in arbitrary shared text and extracts site video IDs (duplicate matching). `SongAdder` reads a link into a `LinkPreview`, saves the song, and enqueues a download. `FileImporter` handles WhatsApp/local files (SHA-256 fingerprint for duplicates, ffmpeg `-vn -c:a copy` for video). `DownloadScheduler` + `DownloadWorker` (WorkManager, one unique job per song): temporary failures retry up to 7 days, permanent ones (`DownloadError.Permanent`) stop immediately.
- **`downloader/`** — Wraps youtubedl-android. `NativeTools` unpacks Python/ffmpeg/QuickJS; `YtDlp` reads info and downloads; `ScriptStore` keeps current / previous / skipped yt-dlp script versions in no-backup storage; `YtDlpUpdater` does the daily self-update (checksum → side-by-side test on 3 links → switch) and rollback after a streak of site failures. All site-specific logic belongs in yt-dlp, not app code.
- **Play-while-downloading (ADD-11)** spans three files: `DownloadWorker` prefers `HttpDownloader` for direct audio-only formats (falling back to yt-dlp's downloader for HLS/DASH or when refused), writing a part file and reporting bytes to `ActiveDownloads`; `player/SongDataSource` (a Media3 DataSource) reads the part file and blocks on `ActiveDownloads` for more bytes, so data is fetched once.
- **`player/`** — `PlaybackService` is a Media3 `MediaLibraryService` (lock screen, notification, Bluetooth, Android Auto via `LibraryBrowser`); it restores state paused and must never auto-start playback (PLY-9). Media items travel as song IDs and are resolved in the service. `PlayerConnection` is the UI-side MediaController exposing state flows, queue / Up Next edits, and removing songs from the queue when they leave the library. `AppContainer.playbackActive` gates silent app updates.
- **`update/`** — `AppUpdater` checks GitHub Releases and installs via PackageInstaller with `USER_ACTION_NOT_REQUIRED`, deferred while music plays; `DailyWorker` runs the app and yt-dlp checks.
- **`photo/`** — `DrawingMaker` (FaceDetector crop) + `Xdog` (line-drawing filter, parameters specified in requirements §7) turn the user's photo into a drawing; `PhotoStore` keeps only the drawing; `HomeShortcut` manages the pinned shortcut that carries the drawing and name.
- **`setup/PhoneSetup`** — checks / settings intents for install-unknown-apps, battery, Xiaomi Autostart, Android Auto.
- **`ui/`** — Compose with Navigation (`Routes.kt` lists every route; `from` args distinguish welcome vs settings flows). `UiState` holds cross-screen state: `MessageCenter`, incoming shares (from `MainActivity` share intents → Add Song), photo draft. `theme/` maps the four design tokens onto Material3 and computes font scale = phone scale × Text Size, capped at 2.

## UI and copy rules (from the design system)

- Four colours only (`surface`, `fill`, `ink`, `accent`), flat: no borders, outlines, dividers, shadows or elevation; no `OutlinedButton`. One `accent` shape per screen (the main action). Light theme only, ignore dark mode.
- Every button has icon + text label; touch targets ≥ 64dp, 16dp apart; never swipe-only or long-press-only; Move Up / Move Down beside any drag. Never disable a button to mean "not yet" — show a Message instead.
- Text never truncates (no ellipsis); layouts must hold at 2× font scale. Atkinson Hyperlegible Next only; icons are Material Symbols Rounded weight 500 as `res/drawable/ic_*.xml`.
- Copy: plain words, address the user as "you", Title Case buttons, sentence-case messages without exclamation marks or error codes ("Will download later", "Can't be saved", "Up Next" — never "queue", "retry", "error", "buffer").
