# My Music App

A calm, distraction-free music player that keeps your songs on your phone. Save songs you find online or receive on WhatsApp, keep them in simple lists, and play them with one tap. Everything stays on the phone; there are no ads, accounts or servers.

Android 15 or newer. Made for Xiaomi, Redmi and Poco phones, and works on others too.

## Install the app (first time only)

Someone helping the listener does this once. After this, the app updates itself.

1. On the phone, open **https://codetiger.github.io/My-Music-App/** in Chrome.
2. Tap **Download the App**. If Chrome says the file might be harmful, tap **Download anyway**. (The APK is also under **Releases** on this page.)
3. When the download finishes, tap **Open**. If you missed it, open the **Files** or **Downloads** app and tap the file.
4. The phone says installing from this source isn't allowed. Tap **Settings**, turn on **Allow from this source**, then go back.
   - On Xiaomi, Redmi and Poco phones, the phone may also show a security check. Wait for it, then tap **Continue** or **Install anyway**. If it asks you to sign in to a Mi account, tap **Cancel** and try **Install anyway** again.
5. Tap **Install**, then **Open**.
6. The app asks for the listener's name and photo, then walks through **Phone Setup**. Do every step: it keeps music playing with the screen off and lets the app update itself.

If Android Auto is used in the car, Phone Setup shows the extra steps for it.

## What it does

- **Add Song**: paste or share a YouTube, YouTube Music, Facebook or Instagram link, or a link to an MP3 file. The link can sit inside a forwarded WhatsApp message. Songs, videos and voice notes shared from WhatsApp, and audio files on the phone, can be added too. Every song is saved on the phone, so it plays without internet.
- **Home**: one big Play button, your lists (Favourites, Recently Played and your own), and every song with search and sort.
- **Player**: lock screen, notification, Bluetooth and Android Auto controls. The app reopens where it stopped and never starts playing by itself.
- **Forgiving**: removed songs and lists wait 30 days in Recently Removed (Settings → Storage), where **Put Back** restores them.

The full requirements are in [highlevel-requirements.md](highlevel-requirements.md); the look is in [design-system/](design-system/README.md).

## For developers

Native Android app: Kotlin and Jetpack Compose, minSdk 35, target 37, arm64 only.

### Build

Needs JDK 17 and the Android SDK with Platform 37.2 and Build-Tools 37. Open the folder in Android Studio, or from a terminal:

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # install on a connected phone or emulator
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android lint
```

Debug builds write a few lines to logcat under the tag `MyMusic` (download failures). Release builds keep no logs of any kind.

### Release

Updates reach phones from this repo's GitHub Releases (requirements section 4.8):

1. Raise `versionCode` and `versionName` in `app/build.gradle.kts` and commit.
2. Tag and push: `git tag v0.2.0 && git push origin v0.2.0` (the tag must be `v<versionName>`).
3. The **Release** workflow (`.github/workflows/release.yml`) runs the tests, builds the signed APK, and publishes a GitHub release with `MyMusicApp.apk` and its SHA-256. The app checks once a day, compares the tag with its own version, and only installs an APK with this package name and a higher `versionCode`.

The workflow signs with four repository secrets: `RELEASE_KEYSTORE_BASE64` (`base64 -i release.jks`), `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`. To build a release locally instead, put `keystore.properties` in the project root (not in git) and run `./gradlew assembleRelease`:

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Other workflows: **CI** runs unit tests, lint and a debug build on every push and pull request; **Pages** publishes `site/` (the download page) to GitHub Pages when it changes. The page reads the newest release from the GitHub API and links to `releases/latest/download/MyMusicApp.apk`, so it needs no update per release. One-time setup: Settings → Pages → Source: **GitHub Actions**.

Every update must be signed with the same key. Keep the keystore and its password backed up outside GitHub; losing them breaks updates for everyone.

The app updates yt-dlp by itself from yt-dlp's own releases (checksum, then a side-by-side test on three links, with the previous version kept for switching back). A new APK is only needed for app changes.

### Layout

| Path | What |
| --- | --- |
| `app/src/main/java/com/codetiger/mymusicapp/data/` | Room database (songs, playlists), settings and player state (DataStore), library logic |
| `.../add/` | Finding links in text, reading them, duplicates, WhatsApp and file import, the download worker |
| `.../downloader/` | yt-dlp, Python and ffmpeg runner, yt-dlp version store and updater, the HTTP downloader |
| `.../player/` | Media3 playback service, Android Auto browse tree, play-while-downloading data source, the app's player connection |
| `.../photo/` | Photo to line drawing (XDoG), the drawing store, the home-screen shortcut |
| `.../setup/`, `.../update/` | Phone Setup checks; app updates and the daily job |
| `.../ui/` | Theme (design tokens), components, screens, navigation |
| `app/src/test/` | Unit tests |
| `site/` | The APK download page on GitHub Pages |
| `.github/workflows/` | CI, release and Pages workflows |
| `app/src/main/res/drawable/ic_*.xml` | Material Symbols Rounded icons (weight 500) |
| `app/src/main/res/font/` | Atkinson Hyperlegible Next (OFL, see `licenses/`) |
| `design-system/` | The design system the screens follow |

### Licence

GPL-3.0 (see [LICENSE](LICENSE)). The downloader comes from [youtubedl-android](https://github.com/yausername/youtubedl-android) (GPL-3.0), which bundles [yt-dlp](https://github.com/yt-dlp/yt-dlp), Python, ffmpeg and QuickJS.
