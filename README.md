# Audex Revival

A native Kotlin revival of **Audex: Accessibility Redefined** — the discontinued 2017
Android accessibility app that gives blind and low-vision users non-speech sound
feedback for what happens on screen.

The original APK (`in.delight.Audex`, 5.8.9, Xamarin.Android, targetSdk 25) was
decompiled and its behavior reimplemented for current Android. The original sound
packs and launcher icon are preserved; the abandoned cloud stack, anti-piracy code,
privileged auto-enable hack, storage permissions and dead links are gone.

- **Package:** `com.infinity.audex_revivel` (installs alongside the original app)
- **Version:** 6.0.0-revival (versionCode 600)
- **Android:** 6.0+ (`minSdk 23`), tested on Android 13 and built against API 36
- **Network:** none. The app has no internet permission and collects no data.

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
- Persistent suspend switch; reset to defaults.

## Install

Build the APK (below) and sideload it:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

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
| UI | [`ui/`](revival/app/src/main/java/com/infinity/audex_revivel/ui) and [`res/layout/`](revival/app/src/main/res/layout) |
| Original audio | [`res/raw/`](revival/app/src/main/res/raw) |

The decompilation analysis, including the original scheme-to-asset table, is in
[docs/AUDEX_ANALYSIS.md](docs/AUDEX_ANALYSIS.md).

## Verification

- 24 JVM unit tests cover scroll/pitch math, smart-label generation, event
  metadata/defaults, the full scheme-to-asset mapping, and spoken battery/time
  text.
- On an Android 13 (API 33) emulator: install, cold launch, main status, live
  accessibility connection, SLS overlay window, settings/event/volume screens,
  all 15 event rows, the six-scheme picker, and switching to Custom (which reveals
  the Choose buttons).
- Not covered by automation: audible output on a physical device, OEM-specific
  accessibility behavior, and two-finger gestures (they depend on touch
  exploration and the active screen reader).
- The original app's `targetSdk 25` code is not reused; no Xamarin, Firebase, or
  Android Support Library remains.

## Differences from the original

- New package and signing key; the old app is untouched.
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
