# Audex Revival

A native Kotlin revival of **Audex: Accessibility Redefined** — the discontinued 2017
Android accessibility app that gives blind and low-vision users non-speech sound
feedback for what happens on screen.

The original APK (`in.delight.Audex`, 5.8.9, Xamarin.Android, targetSdk 25) was
decompiled and its behavior reimplemented for current Android. The original sound
packs and launcher icon are preserved; the abandoned cloud stack, anti-piracy code,
privileged auto-enable hack, storage permissions and dead links are gone.

- **Package:** `com.infinity.audex_revivel` (installs alongside the original app)
- **Version:** 6.1.0-revival (versionCode 610)
- **Android:** 6.0+ (`minSdk 23`), tested on Android 13 and built against API 36
- **Network:** the optional [theme store](https://github.com/Wasilewsk/audex-themes)
  only. The app reads a public index and downloads the theme you choose over
  HTTPS. No accounts, analytics, ads or uploads; nothing about you is sent.

## Features

- Sound feedback for click, touch, touch cleared, focus (interactive and
  non-interactive), long press, scroll, gesture start/end, select and window
  changes.
- Charger connected/disconnected and screen on/off sounds.
- The four original schemes — **Innovex**, **Voiceover**, **Talkback** and
  **Classic Talkback** — with every original audio clip.
- **Shared** scheme: drop files named after events into the app's external
  `Audex` folder. **Custom** scheme: pick an audio file per event with the system
  file picker.
- Ten-step event volume, click haptics, startup feedback, and the original
  five-level Audex priority control.
- **Smart labeling (SLS):** a floating button that toggles generated spoken labels
  for unlabeled controls and remembers exclusions.
- Spoken time and battery level on two-finger accessibility gestures.
- **Theme store:** browse community sound themes hosted on GitHub, install them,
  switch to them from the sound-scheme picker, preview them, and remove them.
  Packages are checksum-verified and extracted with path-traversal protection.
- **Share as theme:** export your Custom scheme as a `.audextheme` package, share
  it, and submit it to the community store. See
  [CONTRIBUTING.md](https://github.com/Wasilewsk/audex-themes/blob/main/CONTRIBUTING.md).
- Persistent suspend switch; reset to defaults.

## Install

Build the APK (below) and sideload it:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

A signed APK is attached to the [latest release](https://github.com/Wasilewsk/audex-revival/releases).
Or copy the APK to the device and open it. Then open **Audex Revival** and press
**Turn on Audex Revival**, which opens Accessibility settings. Enable
**Audex Revival** there.

The first launch explains the app and speaks with TalkBack. The accessibility
service is the only permission it needs, plus vibration.

## Build

Requirements: JDK 17+ and an Android SDK with platform 36 and build-tools 35+.

```bash
cd revival
./gradlew testReleaseUnitTest assembleRelease
```

The APK is written to `revival/app/build/outputs/apk/release/app-release.apk`.

### Signing

Release signing is read from `revival/keystore.properties`, which is gitignored:

```properties
storeFile=keystore/audex-revival.jks
storePassword=change-me
keyAlias=audex
keyPassword=change-me
```

Create your own keystore if you do not have the project's local one:

```bash
keytool -genkeypair -keystore keystore/audex-revival.jks -alias audex \
  -keyalg RSA -keysize 2048 -validity 10000
```

Without `keystore.properties`, `assembleRelease` produces an unsigned APK; use
`assembleDebug` for a debug-signed build, or supply your own key.

## How it works

| Area | Implementation |
| --- | --- |
| Accessibility service | [`AudexAccessibilityService.kt`](revival/app/src/main/java/com/infinity/audex_revivel/service/AudexAccessibilityService.kt) |
| Sound schemes / SoundPool | [`SoundEngine.kt`](revival/app/src/main/java/com/infinity/audex_revivel/core/SoundEngine.kt), [`SoundScheme.kt`](revival/app/src/main/java/com/infinity/audex_revivel/core/SoundScheme.kt) |
| Preferences | [`AudexPreferences.kt`](revival/app/src/main/java/com/infinity/audex_revivel/core/AudexPreferences.kt) |
| Smart labeling | [`SmartLabelingOverlay.kt`](revival/app/src/main/java/com/infinity/audex_revivel/service/SmartLabelingOverlay.kt), [`SmartLabeler.kt`](revival/app/src/main/java/com/infinity/audex_revivel/core/SmartLabeler.kt) |
| Theme store | [`ThemeStoreClient.kt`](revival/app/src/main/java/com/infinity/audex_revivel/core/theme/ThemeStoreClient.kt), [`ThemePackageInstaller.kt`](revival/app/src/main/java/com/infinity/audex_revivel/core/theme/ThemePackageInstaller.kt), [`ThemeStoreActivity.kt`](revival/app/src/main/java/com/infinity/audex_revivel/ui/ThemeStoreActivity.kt) |
| Theme export | [`ThemeExporter.kt`](revival/app/src/main/java/com/infinity/audex_revivel/core/theme/ThemeExporter.kt), [`ThemePackager.kt`](revival/app/src/main/java/com/infinity/audex_revivel/core/theme/ThemePackager.kt) |
| UI | [`ui/`](revival/app/src/main/java/com/infinity/audex_revivel/ui) and [`res/layout/`](revival/app/src/main/res/layout) |
| Original audio | [`res/raw/`](revival/app/src/main/res/raw) |

The decompilation analysis, including the original scheme-to-asset table, is in
[docs/AUDEX_ANALYSIS.md](docs/AUDEX_ANALYSIS.md).

## Verification

- 50 JVM unit tests cover scroll/pitch math, smart-label generation, event
  metadata/defaults, the full scheme-to-asset mapping, spoken battery/time text,
  theme manifest and index parsing, download URL safety, and safe package
  extraction and round-trip packaging.
- On an Android 13 (API 33) emulator: install, cold launch, main status, live
  accessibility connection, SLS overlay window, settings/event/volume screens,
  all 15 event rows, the sound-scheme picker with installed themes, and switching
  to Custom (which reveals the Choose buttons).
- Theme store on the emulator: loaded the live GitHub index over HTTPS, listed
  both demo themes, installed **Chime** (SHA-256 checked), applied it from the
  store and from the scheme picker, kept it selected across a process restart,
  played its preview, and removed it with the scheme falling back to Innovex.
- Export on the emulator: packaged a custom scheme as `adb-theme-1.0.0.audextheme`,
  opened the share/upload dialog, and the store's own validator accepted the
  downloaded package.
- Not covered by automation: audible output on a physical device, OEM-specific
  accessibility behavior, the GitHub upload page itself, and two-finger gestures
  (they depend on touch exploration and the active screen reader).
- The original app's `targetSdk 25` code is not reused; no Xamarin, Firebase, or
  Android Support Library remains.

## Differences from the original

- New package and signing key; the old app is untouched.
- Adds `INTERNET`, used only for the theme store. Theme browsing is explicit;
  nothing is fetched until you open the store, and no data is uploaded.
- Community themes are user-supplied audio, not part of this repository. Each
  theme declares its own license in its manifest.
- Firebase/FCM news, clipboard token copying, Play/support links, the anti-piracy
  retaliation, the `su`/`WRITE_SECURE_SETTINGS` auto-enable hack, and the storage
  permissions are removed.
- Shared sounds live in `Android/data/com.infinity.audex_revivel/files/Audex`
  instead of `/sdcard/Audex`; custom sounds use the Storage Access Framework and
  are copied into app storage, so no storage permission is needed on any Android
  version.
- Suspend survives process death; the SLS overlay is removed cleanly on suspend,
  config change and service shutdown.
- Smart labels are normalised ("Save button", "Email edit box") instead of the
  original "'save button' Button".
- `targetSdk 36`, edge-to-edge insets, Material 3 UI, no deprecated Firebase
  services.

## Attribution

Original Audex by **Akash Kakkar / Akash Delight Computings**, 2017. This is an
independent reimplementation from a decompiled copy of the app, published so the
accessibility functionality can keep working. See [NOTICE.md](NOTICE.md) for the
provenance of the bundled sounds and icon.

The original APK and the raw decompiled artifacts are **not** included in this
repository.

## Version 7.0 — build 1024

Settings → **Create new theme** opens a Theme name dialog with Cancel and Okay.
**Audex theme creator** lists all 15 sound events, their assigned files, and
Preview sound / Change sound controls. New drafts start with Innovex sounds;
changes persist separately from the active scheme. **Save** installs the theme
locally so it appears in Sound scheme. **Edit created theme** reopens a draft.
Audio imports support OGG, WAV, MP3, MIDI and M4A, up to 2 MB per sound.
**Upload to theme store** asks for author/license, saves an `.audextheme` through
the Android file picker, and opens the existing GitHub submission page. Store
publication still follows the repository's review/index process.

Settings also includes **Check for updates** and **Check for updates automatically**.
Automatic checks run on accessibility-service startup and every five minutes
while that service runs; Android sleep may delay checks. Grant notifications to
receive a single notification per available release. The update screen shows
release notes with Install later and Install now. Installation requires Android's
install-from-this-source permission and confirmation in the system installer.
Downloads are restricted to the official repository's release assets over HTTPS;
APK package name, higher versionCode, and signing identity are verified before
opening the installer. Failed background checks remain quiet.

Release tags should use `v7.1.0`, or `v7.0-build1025` for another build of 7.0.
Attach the signed APK as a release asset. The existing project keystore is
required for updates to an installed release; an unsigned release APK cannot
be installed. For a separate local emulator copy:

```sh
cd revival
./gradlew testDebugUnitTest assembleDebug -PstudioTest=true
```

This produces **Audex Revival 7.0 Test** (`com.infinity.audex_revivel.studiotest`).
The test copy cannot install production-release updates because its package and
signing identity differ.
