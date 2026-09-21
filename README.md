# CacaMusicPlayer

A classic **red + white** Android music player. It is meant to feel like a Google-style music app from around **2013** (Holo / early Material), not like a modern Spotify clone.

Local music works **completely offline**. You do not need an account to play files on the phone.

## What it does

- Scans the device for MP3 and other common audio files
- Songs, albums, artists, playlists, favorites, recently played, search
- Play / pause / next / previous / seek / shuffle / repeat
- Background playback and a version-appropriate notification
- Optional **Online Music** using a provider interface (legal sources only)
- Optional account + playlist/favorite sync (off by default)

It will **not** download or unlock protected Spotify / YouTube / YouTube Music streams.

## Android versions

| | |
|---|---|
| Oldest supported | **Android 4.0 Ice Cream Sandwich (API 14)** |
| Target | **Android 15 (API 35)** — runs on phones up to **Android 16** |

Android 2.2 (API 8) is **not** used. Current Android Gradle Plugin and the platform APIs this app needs (Holo ActionBar, `Notification.Builder`, `MediaStore` music columns, runtime-safe storage) are not realistic on Froyo. API 14 is the oldest level that still builds cleanly with modern tools and stays lightweight (no AndroidX, no extra libraries).

## Download the APK from your phone (no PC)

GitHub Actions builds a **release zip** automatically:

1. Open [Actions](https://github.com/vladgaming166-prog/cacamusicplayer/actions).
2. Tap the latest **Release APK** run and wait until it is green / finished.
3. Open **Artifacts**.
4. Download **CacaMusicPlayer-release** (this is a zip).
5. Unzip it and install **CacaMusicPlayer.apk**.
6. If Android blocks the install, allow installs from Chrome / GitHub / your Files app.

You can also tap **Run workflow** on that Actions page if you need a new build.

## How to build the APK on a PC

You need:

1. **JDK 17** or newer  
2. **Android SDK** (install **Android Studio** — that is the easiest way)

### Windows

1. Install [Android Studio](https://developer.android.com/studio).
2. Open this folder once in Android Studio and let it finish downloading the SDK if asked.
3. Double-click **`BUILD.bat`**.
4. When it finishes, the file is:

`app\build\outputs\apk\debug\CacaMusicPlayer.apk`

Copy that APK to a phone and install it (enable “install from unknown sources” if the phone asks).

### Linux / macOS

```bash
chmod +x BUILD.sh gradlew
./BUILD.sh
```

The APK is:

`app/build/outputs/apk/debug/CacaMusicPlayer.apk`

### Android Studio

1. File → Open → this folder  
2. Let Gradle sync  
3. Run **app**, or Build → Build Bundle(s) / APK(s) → Build APK(s)

A **release** APK is also configured. For beginners it is signed with the debug key so you can install it without creating a keystore:

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/CacaMusicPlayer.apk`

For Google Play you must create your own keystore and set `signingConfig` in `app/build.gradle`. The debug key is not for the Play Store.

If Android Studio does not create `local.properties`, copy `local.properties.example` and set `sdk.dir` to your SDK path.

## First run on a phone

1. Open **CacaMusicPlayer**.
2. Allow **music / storage** access when asked. The app needs this only to find songs you already have.
3. It scans the library, or use **Settings → Scan music library**.
4. Open **Songs** and tap a track.

On Android 13+ you may also be asked for notification permission so the playback controls can appear in the status bar.

## Online music (legal)

Open **Online Music**:

- **Internet Archive** — official archive.org search API for items they already offer for streaming
- **Jamendo** — official Creative Commons API (paste a free client ID from [developer.jamendo.com](https://developer.jamendo.com/) in Settings)
- **User catalog** — a JSON file on a server **you** control or have permission to use (set the URL in Settings)
- **Official services** — opens Spotify / YouTube Music / etc. in their own apps. CacaMusicPlayer does not stream their audio

To add another legal source later, implement `MusicProvider` in `app/src/main/java/com/cacamusicplayer/app/online/` and register it in `ProviderRegistry`.

User catalog JSON shape:

```json
{
  "tracks": [
    {
      "title": "Song",
      "artist": "Artist",
      "album": "Album",
      "duration": 180000,
      "streamUrl": "https://example.com/song.mp3",
      "coverUrl": "https://example.com/cover.jpg",
      "source": "My server",
      "license": "CC-BY"
    }
  ]
}
```

## Optional account / sync server

Accounts are optional. Create one in **Account** to store a local profile. Playlists and favorites already live on the device in SQLite.

If you want phone-to-server sync:

```bash
python3 backend/server.py
```

In **Settings**, set **Sync server URL** to `http://YOUR-PC-LAN-IP:8088` then use **Account → Sync**.

The sample catalog is served at `http://YOUR-PC-LAN-IP:8088/catalog.json`.

## Project layout

```
CacaMusicPlayer/
  BUILD.bat / BUILD.sh     automatic APK build
  app/src/main/java/...    Java sources (no Kotlin)
  app/src/main/res/...     2013-style red/white layouts and icons
  backend/                 optional catalog + account sync
```

Main pieces:

- `library/MusicScanner.java` — MediaStore + folder scan
- `data/LibraryStore.java` — SQLite library, playlists, favorites, recents
- `playback/PlaybackService.java` — background `MediaPlayer`
- `online/MusicProvider.java` — plug-in interface for legal online sources
- `account/` — local accounts + optional HTTP sync

No AndroidX, no Glide/Picasso, no OkHttp. The APK stays small enough for 512 MB–1 GB phones.

## Permissions

- Storage / `READ_MEDIA_AUDIO` — find local songs  
- Notifications — playback controls (Android 13+)  
- Internet — only for optional online providers and sync  
- Foreground service — keep music playing after you leave the app  

Local playback, playlists, favorites, recents, and settings work with the network turned off.
