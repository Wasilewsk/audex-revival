# Audex 5.8.9 — decompile analysis

Source artifact: `Audex Installer.apk`

| Field | Value |
| --- | --- |
| Package | `in.delight.Audex` |
| Label | Audex |
| Version | 5.8.9 (versionCode 73) |
| Built | 2017-09-09, Xamarin.Android / Mono |
| minSdk / targetSdk | 15 / 25 |
| Signer | `CN=Akash Kakkar, OU=DroidRevolutions, O=Akash Delight Computings` |
| Certificate MD5 (base64) | `I1JmQunHawyFE2V16lLKJA==` |
| Publisher links | `akashk.in/audex/*`, Play listing `in.delight.Audex` (both dead) |

The APK is a legitimate developer build: its signing certificate matches the first
of the three hashes hard-coded in the app's anti-piracy check. It is not a pirated
repack, but it is abandoned and built for an Android version that predates modern
background, storage, and notification rules.

## What the app does

Audex is an accessibility service that makes Android audible through short
non-speech sounds. It is aimed at blind and low-vision users who want earcons
rather than, or alongside, a screen reader.

- Listens to accessibility events and plays a distinct sound for click, focus
  (interactive/non-interactive), touch exploration start/end, scroll, long press,
  select, gesture begin/end, and window changes.
- Plays system-event sounds for charger connect/disconnect and screen on/off.
- Plays a click haptic when enabled.
- Offers four bundled sound schemes plus user-supplied sounds.
- Provides a Smart Labeling System (SLS): a floating overlay button that toggles
  generated labels for unlabeled controls so they can be announced.
- Speaks the time and battery level on two-finger accessibility gestures.

## Decompiled artifacts

| Path | Contents |
| --- | --- |
| `decompiled/audex/Audex/` | ILSpy C# decompilation of `assemblies/Audex.dll` (15 files, ~4.1k lines) |
| `decompile/apk/` | apktool 3.0.3 decode: manifest, resources, layouts, smali, original assets |
| `decompile/apk/res/raw/` | 60 original sound files (58 ogg + 2 midi) |
| `decompiled/audex/Audex/Resource.cs` | Xamarin resource IDs, including every `R.raw` mapping |

Those paths are from the local decompilation workspace and are intentionally
**not** committed to the public repository; only this analysis and the reimplemented
Kotlin app are published. The original APK is likewise not redistributed.

Key source files:

- `AudexService.cs` — accessibility service, event dispatch, SLS overlay.
- `FunctionsHub.cs` — preferences, SoundPool, scheme selection, anti-piracy gate.
- `MainActivity.cs`, `SettingsActivity.cs`, `EventSettingsActivity.cs`,
  `VolumeSettingsActivity.cs` — UI.
- `SystemEventReceiver.cs` — charger/screen broadcasts.
- `TTS.cs` — time/battery speech.
- `FCMInitialiser.cs`, `AudexFirebaseMessagingService.cs` — dead Firebase news feed.

## Sound scheme mapping

The original loads 15 slots per scheme. The resource IDs in `FunctionsHub.loadSounds`
map to these raw files:

| Slot | Innovex | Voiceover | Talkback | Classic Talkback |
| --- | --- | --- | --- | --- |
| click | click | voclick | tbclick | ctbclick |
| view enter (touch) | viewenter | voviewenter | tbviewenter | ctbviewenter |
| view exit (touch cleared) | viewexit | voviewexit | tbviewexit | ctbviewexit |
| focus interactive | focusinteractive | vofocusinteractive | tbfocusinteractive | ctbfocusinteractive |
| focus non-interactive | focusnoninteractive | vofocusnoninteractive | tbfocusnoninteractive | ctbfocusnoninteractive |
| long click | longclick | volongclick | tblongclick | ctblongclick |
| scroll | scroll | voscroll | tbscroll | ctbscroll |
| gesture begin | gesturebegin | vogesturebegin | tbgesturebegin | ctbgesturebegin |
| gesture end | gestureend | vogestureend | tbgestureend | ctbgestureend |
| select | select | voselect | tbselect | ctbselect |
| window state | windowstate | vowindowstate | tbwindowstate | ctbwindowstate |
| charger plugged | chargerplugged | vochargerplugged | tbchargerplugged | ctbchargerplugged |
| charger unplugged | chargerunplugged | vochargerunplugged | tbchargerunplugged | ctbchargerunplugged |
| screen on | screenunlock | voscreenunlock | tbscreenunlock | ctbscreenunlock |
| screen off | screenlock | voscreenlock | tbscreenlock | ctbscreenlock |

`serviceinitialised.ogg` is the startup/welcome sound. `vo*viewenter.mid` and
`vo*viewexit.mid` are MIDI tracks used by the Voiceover scheme (the ogg files of
the same names are used in the APK; the original code actually points at the ogg
resource IDs for those slots). Unused: `ctbnotificationstate.ogg`, `voviewexit.mid`.

## Preferences (SharedPreferences `AudexPreferences`)

- Event toggles: `clickEvent`, `viewEnterEvent`, `viewExitEvent`,
  `focusInteractiveEvent`, `focusNonInteractiveEvent`, `longPressEvent`,
  `scrollEvent`, `gestureBeginEvent`, `gestureEndEvent`, `selectEvent`,
  `windowStateEvent`, `chargerPluggedEvent`, `chargerUnpluggedEvent`,
  `screenOnEvent`, `screenOffEvent`. All default true except `selectEvent`.
- Custom sound paths: `clickSound`, `viewEnterSound`, `viewExitSound`,
  `focusInteractiveSound`, `focusNonInteractiveSound`, `longPressSound`,
  `scrollSound`, `gestureBeginSound`, `gestureEndSound`, `selectSound`,
  `windowStateSound`, `chargerPluggedSound`, `chargerUnpluggedSound`,
  `screenOnSound`, `screenOffSound`.
- Scheme: `currentSoundScheme` (Innovex / Voiceover / Talkback / Clasic Talkback /
  Shared / Custom).
- Audio: `leftVolume`, `rightVolume` (0.1–1.0).
- Behaviour: `UseSLS`, `DisableAudibleFeedback`, `ClickHapticFeedback`,
  `StartupFeedback`, `audexPriority` (1–5), `ControlsToExcludeFromSLS` (string set).

## Defects and dead code found

1. **Dead cloud stack.** Firebase 2017 FCM registration, topic subscriptions
   (`AudexNews`, `AudexUpdate`, `AudexInternal`), hard-coded Firebase project and
   API key. The instance token is copied to the clipboard on every launch.
2. **Anti-piracy retaliation.** `FunctionsHub` checks the signing certificate MD5.
   On mismatch it speaks and toasts a threat 200 times and vibrates forever. Any
   re-sign of the APK triggers it.
3. **Hard-coded admin device.** `MainActivity.AutoActivateAudex()` tries `su` and
   writes secure settings when `android_id` is a specific value; it also force-
   enables the service via `WRITE_SECURE_SETTINGS`.
4. **Dead links.** Play Store beta, support site, help, changelog and contributors
   links all point at removed resources.
5. **Storage model incompatible with Android 10+.** Shared/custom schemes use
   `WRITE_EXTERNAL_STORAGE` and `_data` column paths; the picker path handling
   crashes if the cursor is null.
6. **Deprecated platform APIs.** targetSdk 25, Android Support Library, legacy
   notifications, FirebaseInstanceIdService.
7. **Preference bug.** `AudexService.loadPreferences` calls
   `windowManager.AddView(floatingView, ...)` even when `floatingView` is null and
   SLS was never populated; `isSuspended` is static and lost on process death.
8. **SLS label noise.** Labels concatenate class suffixes without a space
   ("save Button"), and the overlay is an invisible 3dp-tall strip.
9. **Volume settings crash.** `OnPause` calls `AudexService.loadPreferences()`
   whether or not the service is connected; several activities release a shared
   SoundPool twice.
10. **Gesture TTS requires touch exploration** but the service never requests it,
    so on many devices TalkBack consumes the gestures first.

## Revival decisions

The revival is a native Kotlin application, package `com.infinity.audex_revivel`,
minSdk 23, targetSdk 36, built from the behaviour above:

- Keeps every event, all four bundled schemes, the original audio assets, custom
  per-event sounds, the Shared scheme, SLS, TTS time/battery gestures, charger and
  screen feedback, haptics, volume, priority and event toggles.
- Drops Firebase, the clipboard token leak, the anti-piracy code, the `su`/secure-
  settings hack, the dead links, and all storage permissions.
- Replaces the external `sdcard/Audex` folder with the app-specific external
  directory (`Android/data/com.infinity.audex_revivel/files/Audex`) and uses the
  Storage Access Framework for custom sounds, so no runtime storage permission is
  needed on any supported Android version.
- Persists suspend state, guards SLS against a null overlay, and rebuilds the SLS
  overlay only when its setting changes.
- Uses modern notifications-free operation and targetSdk 36 edge-to-edge insets.
- Keeps the original `AudexPreferences` keys for event toggles, volumes and scheme
  names so the behaviour remains recognisable.
