# freeRASP-Android (PoC)

Minimal Kotlin app integrating Talsec freeRASP 20.0.1 (Talsec Gradle plugin 1.1.0, AGP 9.5.1, Gradle 9.9.0, compileSdk 37).

- `RaspApplication.kt` sets up `TalsecConfig`, registers all listeners and calls `Talsec.start`.
- `RaspConfig.kt` holds every option freeRASP exposes.
- Each callback is shown on screen and logged under the logcat tag `freeRASP-PoC`.
- Debug runs with `prod=false` and release with `prod=true`. Release is minified and signed with the debug key (PoC only).

## Test result (21 Nov 2026, Pixel_9 emulator, Android 17 / API 37, arm64)

Both variants built on the first try, installed and ran with no crash.

| Callback | Debug (prod=false) | Release (prod=true) |
|---|---|---|
| onSimulator | – | +2.7 s |
| onUnofficialStore | – | +1.9 s (sideloaded via adb) |
| onDeviceBinding | – | +4.2 s |
| onAdbEnabled | – | +5.1 s |
| onSecureHardwareNotAvailable | +25.7 s | +3.7 s |
| onPasscode | +26.1 s | +3.7 s |
| onCriticalChecksFinished | +25.6 s | +3.6 s |
| onFastChecksFinished | +34.7 s | +5.2 s |
| onDevMode, onPostponedChecksFinished | not within 60 s | not within 60 s |

The debug timings are slow because the emulator had just cold-booted. Raw logs: `poc-results.txt`. Screenshots: `poc-debug.png`, `poc-release.png`.

## Is it configurable?

Partly. You can configure how the app reacts, but not what gets checked.

**Configurable**
- `prod` true/false: debug, simulator, app-integrity and unofficial-store checks report only when it is true (confirmed above).
- `killOnBypass`: kills the app if the callbacks are hooked.
- `supportedAlternativeStores`: extra trusted installer packages.
- Expected package name and signing-certificate hashes, plus `watcherMail`.
- Per-activity protections: `blockScreenCapture`, `blockScreenControl`, `blockOverlayWindows` (API 31+), screenshot/recording callbacks, and ongoing-call detection.
- Optional `DeviceState` and `RaspExecutionState` listeners.
- What each callback does: the SDK only reports, and the app decides the reaction.

**Not configurable**
- Which checks run. All ~20 threat checks always run; you can only ignore a callback.
- The periodic re-check interval, which the SDK randomises.
- Telemetry to Talsec, which is always on in the free tier.
- The detection code. The core is a closed binary; only the wrapper is MIT.
